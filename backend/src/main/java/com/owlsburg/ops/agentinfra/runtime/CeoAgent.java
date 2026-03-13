package com.owlsburg.ops.agentinfra.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.agentinfra.tools.ToolExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CeoAgent implements Agent {

    public record ChatResult(String response, int inputTokens, int outputTokens,
                             List<ToolCallLog> toolCallLogs, boolean iterationsExhausted) {}

    public record ToolCallLog(String toolName, String input, String result, boolean success) {}

    private static final Logger log = LoggerFactory.getLogger(CeoAgent.class);

    private final AgentIdentity identity;
    private final AgentCapabilities capabilities;
    private final ObjectMapper objectMapper;
    private final AnthropicStreamingClient streamingClient;
    private final CeoToolExecutor toolExecutor;

    public CeoAgent(AgentIdentity identity, AgentCapabilities capabilities,
                    String apiKey, AgentToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.identity = identity;
        this.capabilities = capabilities;
        this.objectMapper = objectMapper;
        this.streamingClient = new AnthropicStreamingClient(apiKey, objectMapper);
        this.toolExecutor = new CeoToolExecutor(toolRegistry, objectMapper, streamingClient);
    }

    @Override
    public AgentIdentity identity() {
        return identity;
    }

    @Override
    public AgentCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public AgentResult execute(AgentContext context, String task) {
        try {
            SseEmitter dummy = new SseEmitter(300_000L);
            dummy.onTimeout(dummy::complete);
            dummy.onError(e -> dummy.complete());
            StringBuilder result = new StringBuilder();
            int[] tokenAccumulator = new int[2];
            List<ToolCallLog> toolCallLogs = new ArrayList<>();
            executeStreamingInternal(context, task, List.of(), dummy, result, tokenAccumulator, toolCallLogs);
            return AgentResult.completed(result.toString(), tokenAccumulator[0] + tokenAccumulator[1], 0, List.of());
        } catch (Exception e) {
            log.error("CEO non-streaming execution error", e);
            return AgentResult.error("Interner Fehler bei der Ausführung");
        }
    }

    @Override
    public void executeStreaming(AgentContext context, String task, SseEmitter emitter) {
        executeStreamingWithHistory(context, task, List.of(), emitter);
    }

    public void executeStreamingWithHistory(AgentContext context, String task,
                                             List<ObjectNode> chatHistory, SseEmitter emitter) {
        StringBuilder fullResponse = new StringBuilder();
        int[] tokenAccumulator = new int[2];
        List<ToolCallLog> toolCallLogs = new ArrayList<>();
        try {
            executeStreamingInternal(context, task, chatHistory, emitter, fullResponse, tokenAccumulator, toolCallLogs);
        } catch (LlmProviderException e) {
            sendErrorEvent(emitter, e.getMessage());
        } catch (Exception e) {
            log.error("CEO streaming error", e);
            sendErrorEvent(emitter, "Interner Fehler");
        }
    }

    public ChatResult getLastResponse(AgentContext context, String task,
                                       List<ObjectNode> chatHistory, SseEmitter emitter) {
        StringBuilder fullResponse = new StringBuilder();
        int[] tokenAccumulator = new int[2];
        List<ToolCallLog> toolCallLogs = new ArrayList<>();
        boolean iterationsExhausted = false;
        try {
            iterationsExhausted = executeStreamingInternal(context, task, chatHistory, emitter,
                    fullResponse, tokenAccumulator, toolCallLogs);
        } catch (Exception e) {
            log.error("CEO streaming error", e);
            sendErrorEvent(emitter, "Interner Fehler");
        }
        return new ChatResult(fullResponse.toString(), tokenAccumulator[0], tokenAccumulator[1],
                toolCallLogs, iterationsExhausted);
    }

    /**
     * Core ReAct loop. Returns true if iterations were exhausted without a natural end.
     */
    private boolean executeStreamingInternal(AgentContext context, String task,
                                              List<ObjectNode> chatHistory, SseEmitter emitter,
                                              StringBuilder fullResponse, int[] tokenAccumulator,
                                              List<ToolCallLog> toolCallLogs) throws Exception {
        List<ObjectNode> messages = new ArrayList<>(chatHistory);

        ToolExecutionContext toolContext = new ToolExecutionContext(
                context.tenantId(), identity.instanceId(), null, context.activityBus(), context.runMemory());

        for (int iteration = 0; iteration < capabilities.maxIterations(); iteration++) {
            publishActivity(context, AgentActivityEvent.Type.THINKING);

            // Inject RunMemory summary into system prompt if available
            String effectiveSystemPrompt = identity.systemPrompt();
            if (context.runMemory() != null && !context.runMemory().isEmpty()) {
                effectiveSystemPrompt = effectiveSystemPrompt + "\n\n" + context.runMemory().buildSummary();
            }

            AnthropicStreamingClient.StreamResult streamResult = streamingClient.streamRequest(
                    identity.model(), effectiveSystemPrompt, messages,
                    capabilities.toolDefinitions(), capabilities.maxTokensPerRun(),
                    capabilities.temperature(), emitter);

            fullResponse.append(streamResult.text());
            tokenAccumulator[0] += streamResult.inputTokens();
            tokenAccumulator[1] += streamResult.outputTokens();

            if ("client_disconnected".equals(streamResult.stopReason())) {
                publishActivity(context, AgentActivityEvent.Type.IDLE);
                return false;
            }

            if (!"tool_use".equals(streamResult.stopReason()) || streamResult.toolUses().isEmpty()) {
                publishActivity(context, AgentActivityEvent.Type.IDLE);
                return false;
            }

            // Build assistant message with all content blocks
            ObjectNode assistantMsg = buildAssistantMessage(streamResult);
            messages.add(assistantMsg);

            // Execute tools via CeoToolExecutor
            CeoToolExecutor.ToolExecutionResult toolResult =
                    toolExecutor.executeTools(streamResult.toolUses(), toolContext, context, identity, emitter);

            toolCallLogs.addAll(toolResult.toolCallLogs());

            // Build tool result message for Anthropic API
            ObjectNode toolResultMsg = buildToolResultMessage(streamResult.toolUses(), toolResult.results());
            messages.add(toolResultMsg);
        }

        // Iterations exhausted without natural end
        log.warn("CEO Agent '{}' erreichte max Iterationen ({}) ohne natürliches Ende",
                identity.name(), capabilities.maxIterations());
        return true;
    }

    private ObjectNode buildAssistantMessage(AnthropicStreamingClient.StreamResult streamResult) {
        ObjectNode assistantMsg = objectMapper.createObjectNode();
        assistantMsg.put("role", "assistant");
        ArrayNode contentBlocks = assistantMsg.putArray("content");

        if (!streamResult.text().isEmpty()) {
            ObjectNode textBlock = contentBlocks.addObject();
            textBlock.put("type", "text");
            textBlock.put("text", streamResult.text());
        }

        for (AnthropicStreamingClient.ToolUseBlock toolUse : streamResult.toolUses()) {
            ObjectNode toolUseBlock = contentBlocks.addObject();
            toolUseBlock.put("type", "tool_use");
            toolUseBlock.put("id", toolUse.id());
            toolUseBlock.put("name", toolUse.name());
            try {
                toolUseBlock.set("input", objectMapper.readTree(toolUse.input()));
            } catch (Exception e) {
                log.warn("Failed to parse tool input JSON for '{}': {}", toolUse.name(), e.getMessage());
                toolUseBlock.putObject("input");
            }
        }
        return assistantMsg;
    }

    private ObjectNode buildToolResultMessage(List<AnthropicStreamingClient.ToolUseBlock> toolUses,
                                               Map<String, String> results) {
        ObjectNode toolResultMsg = objectMapper.createObjectNode();
        toolResultMsg.put("role", "user");
        ArrayNode toolResultBlocks = toolResultMsg.putArray("content");

        for (AnthropicStreamingClient.ToolUseBlock toolUse : toolUses) {
            ObjectNode resultBlock = toolResultBlocks.addObject();
            resultBlock.put("type", "tool_result");
            resultBlock.put("tool_use_id", toolUse.id());
            resultBlock.put("content", results.getOrDefault(toolUse.id(),
                    "Fehler: Kein Ergebnis erhalten (Timeout?)"));
        }
        return toolResultMsg;
    }

    private void publishActivity(AgentContext context, AgentActivityEvent.Type type) {
        if (context.activityBus() == null) return;
        try {
            context.activityBus().publish(context.tenantId(), new AgentActivityEvent(
                    type, identity.instanceId(), null, identity.name(),
                    null, Instant.now()));
        } catch (Exception e) {
            log.debug("Failed to publish activity event: {}", e.getMessage());
        }
    }

    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(
                    "{\"error\":" + objectMapper.writeValueAsString(message) + "}"));
        } catch (Exception e) {
            log.debug("SSE error event send failed: {}", e.getMessage());
        }
    }
}
