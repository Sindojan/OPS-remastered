package com.owlsburg.ops.agentinfra.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
import com.owlsburg.ops.agentinfra.llm.LlmToolDefinition;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.agentinfra.tools.ToolExecutionContext;
import com.owlsburg.ops.agentinfra.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CeoAgent implements Agent {

    public record ChatResult(String response, int inputTokens, int outputTokens,
                                 List<ToolCallLog> toolCallLogs) {}

    public record ToolCallLog(String toolName, String input, String result, boolean success) {}

    private static final Logger log = LoggerFactory.getLogger(CeoAgent.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {1000, 3000, 8000};

    private final AgentIdentity identity;
    private final AgentCapabilities capabilities;
    private final String apiKey;
    private final AgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public CeoAgent(AgentIdentity identity, AgentCapabilities capabilities,
                    String apiKey, AgentToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.identity = identity;
        this.capabilities = capabilities;
        this.apiKey = apiKey;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
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
        // CEO streamt immer — Fallback für nicht-streaming Aufrufer
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

    /**
     * Streaming execution with pre-loaded chat history (as ObjectNode messages).
     */
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

    /**
     * Returns the accumulated full response text with token usage.
     */
    public ChatResult getLastResponse(AgentContext context, String task,
                                       List<ObjectNode> chatHistory, SseEmitter emitter) {
        StringBuilder fullResponse = new StringBuilder();
        int[] tokenAccumulator = new int[2]; // [inputTokens, outputTokens]
        List<ToolCallLog> toolCallLogs = new ArrayList<>();
        try {
            executeStreamingInternal(context, task, chatHistory, emitter, fullResponse, tokenAccumulator, toolCallLogs);
        } catch (Exception e) {
            log.error("CEO streaming error", e);
            sendErrorEvent(emitter, "Interner Fehler");
        }
        return new ChatResult(fullResponse.toString(), tokenAccumulator[0], tokenAccumulator[1], toolCallLogs);
    }

    private void executeStreamingInternal(AgentContext context, String task,
                                           List<ObjectNode> chatHistory, SseEmitter emitter,
                                           StringBuilder fullResponse, int[] tokenAccumulator,
                                           List<ToolCallLog> toolCallLogs) throws Exception {
        List<ObjectNode> messages = new ArrayList<>(chatHistory);

        ToolExecutionContext toolContext = new ToolExecutionContext(
                context.tenantId(), identity.instanceId(), null, context.activityBus(), context.runMemory());

        for (int iteration = 0; iteration < capabilities.maxIterations(); iteration++) {
            // Publish THINKING event
            publishActivity(context, AgentActivityEvent.Type.THINKING, null, null);

            // Inject RunMemory summary into system prompt if available
            String effectiveSystemPrompt = identity.systemPrompt();
            if (context.runMemory() != null && !context.runMemory().isEmpty()) {
                effectiveSystemPrompt = effectiveSystemPrompt + "\n\n" + context.runMemory().buildSummary();
            }

            StreamResult streamResult = streamAnthropicRequest(
                    effectiveSystemPrompt, messages, capabilities.toolDefinitions(), emitter);

            fullResponse.append(streamResult.text);
            tokenAccumulator[0] += streamResult.inputTokens;
            tokenAccumulator[1] += streamResult.outputTokens;

            if ("client_disconnected".equals(streamResult.stopReason)) {
                // Client disconnected — abort ReAct loop to save API tokens
                publishActivity(context, AgentActivityEvent.Type.IDLE, null, null);
                break;
            }

            if (!"tool_use".equals(streamResult.stopReason) || streamResult.toolUses.isEmpty()) {
                // Publish IDLE event
                publishActivity(context, AgentActivityEvent.Type.IDLE, null, null);
                break;
            }

            // Build assistant message with all content blocks
            ObjectNode assistantMsg = objectMapper.createObjectNode();
            assistantMsg.put("role", "assistant");
            ArrayNode contentBlocks = assistantMsg.putArray("content");

            if (!streamResult.text.isEmpty()) {
                ObjectNode textBlock = contentBlocks.addObject();
                textBlock.put("type", "text");
                textBlock.put("text", streamResult.text);
            }

            for (ToolUseBlock toolUse : streamResult.toolUses) {
                ObjectNode toolUseBlock = contentBlocks.addObject();
                toolUseBlock.put("type", "tool_use");
                toolUseBlock.put("id", toolUse.id);
                toolUseBlock.put("name", toolUse.name);
                try {
                    toolUseBlock.set("input", objectMapper.readTree(toolUse.input));
                } catch (Exception e) {
                    log.warn("Failed to parse tool input JSON for '{}': {}", toolUse.name, e.getMessage());
                    toolUseBlock.putObject("input");
                }
            }
            messages.add(assistantMsg);

            // Partition tools into delegations and regular tools
            List<ToolUseBlock> delegations = new ArrayList<>();
            List<ToolUseBlock> regularTools = new ArrayList<>();
            for (ToolUseBlock toolUse : streamResult.toolUses) {
                if ("delegate_to_lead".equals(toolUse.name)) {
                    delegations.add(toolUse);
                } else {
                    regularTools.add(toolUse);
                }
            }

            // Results map: toolUseId -> resultContent (preserves insertion order)
            Map<String, String> toolResults = new LinkedHashMap<>();

            // Phase 1: Execute regular tools sequentially
            for (ToolUseBlock toolUse : regularTools) {
                try {
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                            Map.of("toolCall", Map.of("name", toolUse.name, "input", toolUse.input)))));
                } catch (Exception e) {
                    log.debug("SSE toolCall event send failed: {}", e.getMessage());
                }
                publishActivity(context, AgentActivityEvent.Type.TOOL_CALL, null,
                        truncateDetail(toolUse.name));

                String toolResultContent = executeToolSafe(toolContext, toolUse);
                boolean toolSuccess = !toolResultContent.startsWith("Fehler");
                toolResults.put(toolUse.id, toolResultContent);
                toolCallLogs.add(new ToolCallLog(toolUse.name, toolUse.input, toolResultContent, toolSuccess));

                try {
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                            Map.of("toolResult", Map.of("name", toolUse.name, "result", toolResultContent)))));
                } catch (Exception e) {
                    log.debug("SSE toolResult event send failed: {}", e.getMessage());
                }
                publishActivity(context, AgentActivityEvent.Type.TOOL_RESULT, null,
                        truncateDetail(toolUse.name));
            }

            // Phase 2: Execute delegations in parallel via Virtual Threads
            if (!delegations.isEmpty()) {
                // Send all delegation-start events immediately
                for (ToolUseBlock toolUse : delegations) {
                    try {
                        JsonNode delegateInput = objectMapper.readTree(toolUse.input);
                        String leadName = delegateInput.has("lead") ? delegateInput.get("lead").asText() : "unknown";
                        String delegateTask = delegateInput.has("task") ? delegateInput.get("task").asText() : "";
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                                Map.of("delegation", Map.of("lead", leadName, "task", delegateTask,
                                        "status", "running", "id", toolUse.id)))));
                    } catch (Exception e) {
                        log.debug("SSE delegation event send failed: {}", e.getMessage());
                    }
                }

                // Capture TenantContext for Virtual Thread propagation
                String capturedTenantId = context.tenantId();
                ConcurrentLinkedQueue<DelegationResult> delegationResults = new ConcurrentLinkedQueue<>();

                List<Thread> threads = new ArrayList<>();
                for (ToolUseBlock toolUse : delegations) {
                    Thread t = Thread.startVirtualThread(() -> {
                        try {
                            com.owlsburg.ops.common.TenantContext.setCurrentTenant(capturedTenantId);
                            String resultContent = executeToolSafe(toolContext, toolUse);

                            // Parse structured result and emit leadStep events
                            emitLeadStepEvents(emitter, toolUse, resultContent);

                            delegationResults.add(new DelegationResult(toolUse.id, resultContent));
                        } catch (Exception e) {
                            log.error("Parallel delegation error for '{}': {}", toolUse.id, e.getMessage());
                            delegationResults.add(new DelegationResult(toolUse.id,
                                    "Fehler bei der Delegation: " + e.getMessage()));
                        } finally {
                            com.owlsburg.ops.common.TenantContext.clear();
                        }
                    });
                    threads.add(t);
                }

                // Wait for all delegation threads (max 90s each)
                for (Thread t : threads) {
                    try {
                        t.join(90_000);
                        if (t.isAlive()) {
                            t.interrupt();
                            log.warn("Delegation thread timed out after 90s, interrupted");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while waiting for delegation thread");
                    }
                }

                // Collect results, fill missing with timeout error
                for (DelegationResult dr : delegationResults) {
                    toolResults.put(dr.toolUseId, dr.resultContent);
                }
                for (ToolUseBlock toolUse : delegations) {
                    String delegResult = toolResults.get(toolUse.id);
                    if (delegResult == null) {
                        delegResult = "Fehler: Delegation-Timeout nach 90 Sekunden";
                        toolResults.put(toolUse.id, delegResult);
                    }
                    toolCallLogs.add(new ToolCallLog(toolUse.name, toolUse.input, delegResult, !delegResult.startsWith("Fehler")));
                }
            }

            // Build tool result message for Anthropic API
            ObjectNode toolResultMsg = objectMapper.createObjectNode();
            toolResultMsg.put("role", "user");
            ArrayNode toolResultBlocks = toolResultMsg.putArray("content");

            for (ToolUseBlock toolUse : streamResult.toolUses) {
                ObjectNode resultBlock = toolResultBlocks.addObject();
                resultBlock.put("type", "tool_result");
                resultBlock.put("tool_use_id", toolUse.id);
                resultBlock.put("content", toolResults.getOrDefault(toolUse.id,
                        "Fehler: Kein Ergebnis erhalten (Timeout?)"));
            }
            messages.add(toolResultMsg);
        }
    }

    private StreamResult streamAnthropicRequest(
            String systemPrompt, List<ObjectNode> messages,
            List<LlmToolDefinition> tools, SseEmitter emitter) throws Exception {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", identity.model());
        body.put("max_tokens", capabilities.maxTokensPerRun());
        body.put("system", systemPrompt);
        body.put("stream", true);

        ArrayNode messagesArray = body.putArray("messages");
        for (ObjectNode msg : messages) {
            messagesArray.add(msg);
        }

        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (LlmToolDefinition tool : tools) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("name", tool.name());
                toolNode.put("description", tool.description());
                try {
                    toolNode.set("input_schema", objectMapper.readTree(tool.inputSchema()));
                } catch (Exception e) {
                    toolNode.putObject("input_schema");
                }
            }
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<java.io.InputStream> response = sendWithRetry(httpRequest);

        StringBuilder textContent = new StringBuilder();
        List<ToolUseBlock> toolUses = new ArrayList<>();
        String stopReason = "end_turn";
        int inputTokens = 0;
        int outputTokens = 0;

        String currentToolId = null;
        String currentToolName = null;
        StringBuilder currentToolInput = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) break;

                JsonNode event = objectMapper.readTree(data);
                String type = event.has("type") ? event.get("type").asText() : "";

                switch (type) {
                    case "message_start" -> {
                        JsonNode msg = event.get("message");
                        if (msg != null && msg.has("usage")) {
                            inputTokens = msg.get("usage").path("input_tokens").asInt(0);
                        }
                    }
                    case "content_block_start" -> {
                        JsonNode contentBlock = event.get("content_block");
                        if (contentBlock != null && "tool_use".equals(contentBlock.get("type").asText())) {
                            currentToolId = contentBlock.get("id").asText();
                            currentToolName = contentBlock.get("name").asText();
                            currentToolInput.setLength(0);
                        }
                    }
                    case "content_block_delta" -> {
                        JsonNode delta = event.get("delta");
                        if (delta == null) continue;

                        String deltaType = delta.get("type").asText();
                        if ("text_delta".equals(deltaType)) {
                            String token = delta.get("text").asText();
                            textContent.append(token);
                            if (!trySend(emitter, "{\"token\":" + objectMapper.writeValueAsString(token) + "}")) {
                                log.info("Client disconnected during streaming, aborting");
                                return new StreamResult(textContent.toString(), "client_disconnected", toolUses, inputTokens, outputTokens);
                            }
                        } else if ("input_json_delta".equals(deltaType)) {
                            String partialJson = delta.get("partial_json").asText();
                            currentToolInput.append(partialJson);
                        }
                    }
                    case "content_block_stop" -> {
                        if (currentToolId != null) {
                            String inputJson = currentToolInput.toString();
                            if (inputJson.isEmpty()) inputJson = "{}";
                            toolUses.add(new ToolUseBlock(currentToolId, currentToolName, inputJson));
                            currentToolId = null;
                            currentToolName = null;
                            currentToolInput.setLength(0);
                        }
                    }
                    case "message_delta" -> {
                        JsonNode delta = event.get("delta");
                        if (delta != null && delta.has("stop_reason")) {
                            stopReason = delta.get("stop_reason").asText();
                        }
                        JsonNode usage = event.get("usage");
                        if (usage != null) {
                            outputTokens = usage.path("output_tokens").asInt(0);
                        }
                    }
                }
            }
        }

        return new StreamResult(textContent.toString(), stopReason, toolUses, inputTokens, outputTokens);
    }

    private HttpResponse<java.io.InputStream> sendWithRetry(HttpRequest httpRequest) throws Exception {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            HttpResponse<java.io.InputStream> response = HTTP_CLIENT.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                return response;
            }

            String errorBody;
            try (java.io.InputStream errStream = response.body()) {
                errorBody = new String(errStream.readAllBytes());
            }

            int status = response.statusCode();
            if (isRetryable(status) && attempt < MAX_RETRIES) {
                log.warn("Anthropic streaming API returned {} (attempt {}/{}), retrying in {}ms...",
                        status, attempt + 1, MAX_RETRIES + 1, RETRY_DELAYS_MS[attempt]);
                try {
                    Thread.sleep(RETRY_DELAYS_MS[attempt]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LlmProviderException("Unterbrochen während Retry");
                }
                continue;
            }

            log.error("Anthropic API error ({}): {}", status, errorBody);
            throw new LlmProviderException("Anthropic API Fehler: HTTP " + status);
        }
        throw new LlmProviderException("Anthropic API nicht erreichbar nach " + (MAX_RETRIES + 1) + " Versuchen");
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode == 529 || statusCode == 500
                || statusCode == 502 || statusCode == 503;
    }

    /**
     * Attempts to send data via SSE. Returns false if client is disconnected.
     */
    private boolean trySend(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
            return true;
        } catch (Exception e) {
            log.debug("SSE send failed (client disconnected?): {}", e.getMessage());
            return false;
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

    private String executeToolSafe(ToolExecutionContext toolContext, ToolUseBlock toolUse) {
        try {
            AgentTool tool = toolRegistry.getTool(toolUse.name)
                    .orElseThrow(() -> new IllegalArgumentException("Tool nicht gefunden: " + toolUse.name));
            ToolResult result = tool.execute(toolContext, toolUse.input);
            return result.success() ? result.data() : "Fehler: " + result.errorMessage();
        } catch (Exception e) {
            log.error("Tool execution error for '{}': {}", toolUse.name, e.getMessage());
            return "Fehler bei Tool-Ausführung: " + e.getMessage();
        }
    }

    private void emitLeadStepEvents(SseEmitter emitter, ToolUseBlock toolUse, String resultContent) {
        try {
            JsonNode delegateInput = objectMapper.readTree(toolUse.input);
            String leadName = delegateInput.has("lead") ? delegateInput.get("lead").asText() : "unknown";

            // Try to parse structured result with steps
            JsonNode resultJson = objectMapper.readTree(resultContent);
            String output = resultContent;
            if (resultJson.has("output") && resultJson.has("steps")) {
                output = resultJson.get("output").asText();

                // Emit leadStep events for each step
                JsonNode stepsArray = resultJson.get("steps");
                for (JsonNode step : stepsArray) {
                    Map<String, Object> stepEvent = new LinkedHashMap<>();
                    stepEvent.put("lead", leadName);
                    stepEvent.put("type", step.get("type").asText());
                    if (step.has("toolName") && !step.get("toolName").isNull()) {
                        stepEvent.put("toolName", step.get("toolName").asText());
                    }
                    stepEvent.put("content", step.get("content").asText());
                    stepEvent.put("iteration", step.get("iteration").asInt());
                    stepEvent.put("id", toolUse.id);

                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                            Map.of("leadStep", stepEvent))));
                }
            }

            // Emit delegationResult with only the output
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                    Map.of("delegationResult", Map.of("lead", leadName, "result", output, "id", toolUse.id)))));

        } catch (Exception e) {
            log.debug("Failed to emit leadStep/delegationResult events: {}", e.getMessage());
        }
    }

    private void publishActivity(AgentContext context, AgentActivityEvent.Type type,
                                  java.util.UUID targetInstanceId, String detail) {
        if (context.activityBus() == null) return;
        try {
            context.activityBus().publish(context.tenantId(), new AgentActivityEvent(
                    type, identity.instanceId(), targetInstanceId, identity.name(),
                    detail, Instant.now()));
        } catch (Exception e) {
            log.debug("Failed to publish activity event: {}", e.getMessage());
        }
    }

    private static String truncateDetail(String text) {
        if (text == null) return null;
        return text.length() > 120 ? text.substring(0, 120) : text;
    }

    private record StreamResult(String text, String stopReason, List<ToolUseBlock> toolUses, int inputTokens, int outputTokens) {}
    private record ToolUseBlock(String id, String name, String input) {}
    private record DelegationResult(String toolUseId, String resultContent) {}
}
