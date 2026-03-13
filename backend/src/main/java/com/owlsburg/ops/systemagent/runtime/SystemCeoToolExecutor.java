package com.owlsburg.ops.systemagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.runtime.AgentActivityEvent;
import com.owlsburg.ops.agentinfra.runtime.AnthropicStreamingClient;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemAgentToolRegistry;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Executes tools and delegations for the System CEO agent.
 * No TenantContext needed – system-level execution.
 */
public final class SystemCeoToolExecutor {

    public record ToolExecutionResult(Map<String, String> results,
                                       List<SystemCeoAgent.ToolCallLog> toolCallLogs) {}

    private static final Logger log = LoggerFactory.getLogger(SystemCeoToolExecutor.class);
    static final int MAX_PARALLEL_DELEGATIONS = 5;
    static final long DELEGATION_TIMEOUT_MS = 90_000;

    private final SystemAgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public SystemCeoToolExecutor(SystemAgentToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    public ToolExecutionResult executeTools(List<AnthropicStreamingClient.ToolUseBlock> toolUses,
                                             SystemToolExecutionContext toolContext,
                                             SystemAgentContext agentContext,
                                             SystemAgentIdentity identity,
                                             SseEmitter emitter) {

        List<AnthropicStreamingClient.ToolUseBlock> delegations = new ArrayList<>();
        List<AnthropicStreamingClient.ToolUseBlock> regularTools = new ArrayList<>();
        for (AnthropicStreamingClient.ToolUseBlock toolUse : toolUses) {
            if ("delegate_to_system_lead".equals(toolUse.name())) {
                delegations.add(toolUse);
            } else {
                regularTools.add(toolUse);
            }
        }

        Map<String, String> toolResults = new LinkedHashMap<>();
        List<SystemCeoAgent.ToolCallLog> toolCallLogs = new ArrayList<>();

        // Phase 1: Execute regular tools sequentially
        for (AnthropicStreamingClient.ToolUseBlock toolUse : regularTools) {
            emitToolCallEvent(emitter, toolUse);
            publishActivity(agentContext, AgentActivityEvent.Type.TOOL_CALL, identity, toolUse.name());

            String toolResultContent = executeToolSafe(toolContext, toolUse);
            boolean toolSuccess = !toolResultContent.startsWith("Fehler");
            toolResults.put(toolUse.id(), toolResultContent);
            toolCallLogs.add(new SystemCeoAgent.ToolCallLog(toolUse.name(), toolUse.input(), toolResultContent, toolSuccess));

            emitToolResultEvent(emitter, toolUse, toolResultContent);
            publishActivity(agentContext, AgentActivityEvent.Type.TOOL_RESULT, identity, toolUse.name());
        }

        // Phase 2: Execute delegations in parallel via Virtual Threads
        if (!delegations.isEmpty()) {
            List<AnthropicStreamingClient.ToolUseBlock> activeDelegations =
                    delegations.size() > MAX_PARALLEL_DELEGATIONS
                            ? delegations.subList(0, MAX_PARALLEL_DELEGATIONS)
                            : delegations;

            if (delegations.size() > MAX_PARALLEL_DELEGATIONS) {
                for (int i = MAX_PARALLEL_DELEGATIONS; i < delegations.size(); i++) {
                    AnthropicStreamingClient.ToolUseBlock excess = delegations.get(i);
                    String errorMsg = "Fehler: Maximale parallele Delegationen (" + MAX_PARALLEL_DELEGATIONS + ") überschritten";
                    toolResults.put(excess.id(), errorMsg);
                    toolCallLogs.add(new SystemCeoAgent.ToolCallLog(excess.name(), excess.input(), errorMsg, false));
                }
            }

            for (AnthropicStreamingClient.ToolUseBlock toolUse : activeDelegations) {
                emitDelegationStartEvent(emitter, toolUse);
            }

            // No TenantContext propagation needed for system agents
            ConcurrentLinkedQueue<DelegationResult> delegationResults = new ConcurrentLinkedQueue<>();
            List<Thread> threads = new ArrayList<>();

            for (AnthropicStreamingClient.ToolUseBlock toolUse : activeDelegations) {
                Thread t = Thread.ofVirtual().start(() -> {
                    try {
                        String resultContent = executeToolSafe(toolContext, toolUse);
                        emitLeadStepEvents(emitter, toolUse, resultContent);
                        delegationResults.add(new DelegationResult(toolUse.id(), resultContent));
                    } catch (Exception e) {
                        log.error("System delegation error for '{}': {}", toolUse.id(), e.getMessage());
                        delegationResults.add(new DelegationResult(toolUse.id(),
                                "Fehler bei der Delegation: " + e.getMessage()));
                    }
                });
                threads.add(t);
            }

            for (Thread t : threads) {
                try {
                    t.join(DELEGATION_TIMEOUT_MS);
                    if (t.isAlive()) {
                        t.interrupt();
                        log.warn("System delegation thread timed out after {}ms", DELEGATION_TIMEOUT_MS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            for (DelegationResult dr : delegationResults) {
                toolResults.put(dr.toolUseId, dr.resultContent);
            }
            for (AnthropicStreamingClient.ToolUseBlock toolUse : activeDelegations) {
                String delegResult = toolResults.get(toolUse.id());
                if (delegResult == null) {
                    delegResult = "Fehler: Delegation-Timeout nach " + (DELEGATION_TIMEOUT_MS / 1000) + " Sekunden";
                    toolResults.put(toolUse.id(), delegResult);
                }
                toolCallLogs.add(new SystemCeoAgent.ToolCallLog(toolUse.name(), toolUse.input(), delegResult, !delegResult.startsWith("Fehler")));
            }
        }

        return new ToolExecutionResult(toolResults, toolCallLogs);
    }

    private String executeToolSafe(SystemToolExecutionContext toolContext, AnthropicStreamingClient.ToolUseBlock toolUse) {
        try {
            SystemAgentTool tool = toolRegistry.getTool(toolUse.name())
                    .orElseThrow(() -> new IllegalArgumentException("System-Tool nicht gefunden: " + toolUse.name()));
            SystemToolResult result = tool.execute(toolContext, toolUse.input());
            return result.success() ? result.data() : "Fehler: " + result.errorMessage();
        } catch (Exception e) {
            log.error("System tool execution error for '{}': {}", toolUse.name(), e.getMessage());
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

            JsonNode resultJson = objectMapper.readTree(resultContent);
            String output = resultContent;
            if (resultJson.has("output") && resultJson.has("steps")) {
                output = resultJson.get("output").asText();
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

            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                    Map.of("delegationResult", Map.of("lead", leadName, "result", output, "id", toolUse.id())))));
        } catch (Exception e) {
            log.debug("Failed to emit system leadStep/delegationResult events: {}", e.getMessage());
        }
    }

    private void publishActivity(SystemAgentContext context, AgentActivityEvent.Type type,
                                  SystemAgentIdentity identity, String detail) {
        if (context.activityBus() == null) return;
        try {
            context.activityBus().publish(new AgentActivityEvent(
                    type, identity.instanceId(), null, identity.name(),
                    detail != null && detail.length() > 120 ? detail.substring(0, 120) : detail,
                    Instant.now()));
        } catch (Exception e) {
            log.debug("Failed to publish system activity event: {}", e.getMessage());
        }
    }

    private record DelegationResult(String toolUseId, String resultContent) {}
}
