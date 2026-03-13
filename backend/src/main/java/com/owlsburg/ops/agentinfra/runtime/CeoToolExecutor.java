package com.owlsburg.ops.agentinfra.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.agentinfra.tools.ToolExecutionContext;
import com.owlsburg.ops.agentinfra.tools.ToolResult;
import com.owlsburg.ops.common.TenantAwareExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Executes tools and delegations for the CEO agent.
 * Handles sequential tool execution and parallel lead-agent delegation.
 */
public final class CeoToolExecutor {

    public record ToolExecutionResult(Map<String, String> results,
                                       List<CeoAgent.ToolCallLog> toolCallLogs) {}

    private static final Logger log = LoggerFactory.getLogger(CeoToolExecutor.class);
    static final int MAX_PARALLEL_DELEGATIONS = 5;
    static final long DELEGATION_TIMEOUT_MS = 90_000;

    private final AgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final AnthropicStreamingClient streamingClient;

    public CeoToolExecutor(AgentToolRegistry toolRegistry, ObjectMapper objectMapper,
                           AnthropicStreamingClient streamingClient) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.streamingClient = streamingClient;
    }

    /**
     * Executes all tool uses: regular tools sequentially, delegations in parallel (max 5).
     * Sends SSE events for each tool call/result.
     */
    public ToolExecutionResult executeTools(List<AnthropicStreamingClient.ToolUseBlock> toolUses,
                                             ToolExecutionContext toolContext,
                                             AgentContext agentContext,
                                             AgentIdentity identity,
                                             SseEmitter emitter) {

        // Partition tools into delegations and regular tools
        List<AnthropicStreamingClient.ToolUseBlock> delegations = new ArrayList<>();
        List<AnthropicStreamingClient.ToolUseBlock> regularTools = new ArrayList<>();
        for (AnthropicStreamingClient.ToolUseBlock toolUse : toolUses) {
            if ("delegate_to_lead".equals(toolUse.name())) {
                delegations.add(toolUse);
            } else {
                regularTools.add(toolUse);
            }
        }

        Map<String, String> toolResults = new LinkedHashMap<>();
        List<CeoAgent.ToolCallLog> toolCallLogs = new ArrayList<>();

        // Phase 1: Execute regular tools sequentially
        for (AnthropicStreamingClient.ToolUseBlock toolUse : regularTools) {
            emitToolCallEvent(emitter, toolUse);
            publishActivity(agentContext, AgentActivityEvent.Type.TOOL_CALL, identity, toolUse.name());

            String toolResultContent = executeToolSafe(toolContext, toolUse);
            boolean toolSuccess = !toolResultContent.startsWith("Fehler");
            toolResults.put(toolUse.id(), toolResultContent);
            toolCallLogs.add(new CeoAgent.ToolCallLog(toolUse.name(), toolUse.input(), toolResultContent, toolSuccess));

            emitToolResultEvent(emitter, toolUse, toolResultContent);
            publishActivity(agentContext, AgentActivityEvent.Type.TOOL_RESULT, identity, toolUse.name());
        }

        // Phase 2: Execute delegations in parallel via Virtual Threads (max 5)
        if (!delegations.isEmpty()) {
            List<AnthropicStreamingClient.ToolUseBlock> activeDelegations =
                    delegations.size() > MAX_PARALLEL_DELEGATIONS
                            ? delegations.subList(0, MAX_PARALLEL_DELEGATIONS)
                            : delegations;

            if (delegations.size() > MAX_PARALLEL_DELEGATIONS) {
                log.warn("Capping parallel delegations from {} to {}", delegations.size(), MAX_PARALLEL_DELEGATIONS);
                // Return error for excess delegations
                for (int i = MAX_PARALLEL_DELEGATIONS; i < delegations.size(); i++) {
                    AnthropicStreamingClient.ToolUseBlock excess = delegations.get(i);
                    String errorMsg = "Fehler: Maximale parallele Delegationen (" + MAX_PARALLEL_DELEGATIONS + ") überschritten";
                    toolResults.put(excess.id(), errorMsg);
                    toolCallLogs.add(new CeoAgent.ToolCallLog(excess.name(), excess.input(), errorMsg, false));
                }
            }

            // Send all delegation-start events immediately
            for (AnthropicStreamingClient.ToolUseBlock toolUse : activeDelegations) {
                emitDelegationStartEvent(emitter, toolUse);
            }

            // Execute in parallel
            String capturedTenantId = agentContext.tenantId();
            ConcurrentLinkedQueue<DelegationResult> delegationResults = new ConcurrentLinkedQueue<>();

            List<Thread> threads = new ArrayList<>();
            for (AnthropicStreamingClient.ToolUseBlock toolUse : activeDelegations) {
                Thread t = TenantAwareExecutor.runVirtual(capturedTenantId, () -> {
                    try {
                        String resultContent = executeToolSafe(toolContext, toolUse);
                        emitLeadStepEvents(emitter, toolUse, resultContent);
                        delegationResults.add(new DelegationResult(toolUse.id(), resultContent));
                    } catch (Exception e) {
                        log.error("Parallel delegation error for '{}': {}", toolUse.id(), e.getMessage());
                        delegationResults.add(new DelegationResult(toolUse.id(),
                                "Fehler bei der Delegation: " + e.getMessage()));
                    }
                });
                threads.add(t);
            }

            // Wait for all delegation threads
            for (Thread t : threads) {
                try {
                    t.join(DELEGATION_TIMEOUT_MS);
                    if (t.isAlive()) {
                        t.interrupt();
                        log.warn("Delegation thread timed out after {}ms, interrupted", DELEGATION_TIMEOUT_MS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting for delegation thread");
                }
            }

            // Collect results
            for (DelegationResult dr : delegationResults) {
                toolResults.put(dr.toolUseId, dr.resultContent);
            }
            for (AnthropicStreamingClient.ToolUseBlock toolUse : activeDelegations) {
                String delegResult = toolResults.get(toolUse.id());
                if (delegResult == null) {
                    delegResult = "Fehler: Delegation-Timeout nach " + (DELEGATION_TIMEOUT_MS / 1000) + " Sekunden";
                    toolResults.put(toolUse.id(), delegResult);
                }
                toolCallLogs.add(new CeoAgent.ToolCallLog(toolUse.name(), toolUse.input(), delegResult, !delegResult.startsWith("Fehler")));
            }
        }

        return new ToolExecutionResult(toolResults, toolCallLogs);
    }

    private String executeToolSafe(ToolExecutionContext toolContext, AnthropicStreamingClient.ToolUseBlock toolUse) {
        try {
            AgentTool tool = toolRegistry.getTool(toolUse.name())
                    .orElseThrow(() -> new IllegalArgumentException("Tool nicht gefunden: " + toolUse.name()));
            ToolResult result = tool.execute(toolContext, toolUse.input());
            return result.success() ? result.data() : "Fehler: " + result.errorMessage();
        } catch (Exception e) {
            log.error("Tool execution error for '{}': {}", toolUse.name(), e.getMessage());
            return "Fehler bei Tool-Ausführung: " + e.getMessage();
        }
    }

    private void emitToolCallEvent(SseEmitter emitter, AnthropicStreamingClient.ToolUseBlock toolUse) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                    Map.of("toolCall", Map.of("name", toolUse.name(), "input", toolUse.input())))));
        } catch (Exception e) {
            log.debug("SSE toolCall event send failed: {}", e.getMessage());
        }
    }

    private void emitToolResultEvent(SseEmitter emitter, AnthropicStreamingClient.ToolUseBlock toolUse, String result) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                    Map.of("toolResult", Map.of("name", toolUse.name(), "result", result)))));
        } catch (Exception e) {
            log.debug("SSE toolResult event send failed: {}", e.getMessage());
        }
    }

    private void emitDelegationStartEvent(SseEmitter emitter, AnthropicStreamingClient.ToolUseBlock toolUse) {
        try {
            JsonNode delegateInput = objectMapper.readTree(toolUse.input());
            String leadName = delegateInput.has("lead") ? delegateInput.get("lead").asText() : "unknown";
            String delegateTask = delegateInput.has("task") ? delegateInput.get("task").asText() : "";
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                    Map.of("delegation", Map.of("lead", leadName, "task", delegateTask,
                            "status", "running", "id", toolUse.id())))));
        } catch (Exception e) {
            log.debug("SSE delegation event send failed: {}", e.getMessage());
        }
    }

    private void emitLeadStepEvents(SseEmitter emitter, AnthropicStreamingClient.ToolUseBlock toolUse, String resultContent) {
        try {
            JsonNode delegateInput = objectMapper.readTree(toolUse.input());
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
                    stepEvent.put("id", toolUse.id());

                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                            Map.of("leadStep", stepEvent))));
                }
            }

            // Emit delegationResult with only the output
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                    Map.of("delegationResult", Map.of("lead", leadName, "result", output, "id", toolUse.id())))));

        } catch (Exception e) {
            log.debug("Failed to emit leadStep/delegationResult events: {}", e.getMessage());
        }
    }

    private void publishActivity(AgentContext context, AgentActivityEvent.Type type,
                                  AgentIdentity identity, String detail) {
        if (context.activityBus() == null) return;
        try {
            context.activityBus().publish(context.tenantId(), new AgentActivityEvent(
                    type, identity.instanceId(), null, identity.name(),
                    truncateDetail(detail), java.time.Instant.now()));
        } catch (Exception e) {
            log.debug("Failed to publish activity event: {}", e.getMessage());
        }
    }

    private static String truncateDetail(String text) {
        if (text == null) return null;
        return text.length() > 120 ? text.substring(0, 120) : text;
    }

    private record DelegationResult(String toolUseId, String resultContent) {}
}
