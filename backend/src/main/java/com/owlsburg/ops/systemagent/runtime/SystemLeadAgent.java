package com.owlsburg.ops.systemagent.runtime;

import com.owlsburg.ops.agentinfra.llm.*;
import com.owlsburg.ops.agentinfra.runtime.AgentActivityEvent;
import com.owlsburg.ops.agentinfra.runtime.LeadStep;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemAgentToolRegistry;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SystemLeadAgent implements SystemAgent {

    private static final Logger log = LoggerFactory.getLogger(SystemLeadAgent.class);

    private final SystemAgentIdentity identity;
    private final SystemAgentCapabilities capabilities;
    private final LlmProvider llmProvider;
    private final String apiKey;
    private final SystemAgentToolRegistry toolRegistry;

    public SystemLeadAgent(SystemAgentIdentity identity, SystemAgentCapabilities capabilities,
                           LlmProvider llmProvider, String apiKey, SystemAgentToolRegistry toolRegistry) {
        this.identity = identity;
        this.capabilities = capabilities;
        this.llmProvider = llmProvider;
        this.apiKey = apiKey;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public SystemAgentIdentity identity() {
        return identity;
    }

    @Override
    public SystemAgentCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public SystemAgentResult execute(SystemAgentContext context, String task) {
        try {
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.user(task));

            SystemToolExecutionContext toolContext = new SystemToolExecutionContext(
                    identity.instanceId(), null, context.activityBus(), context.runMemory());

            List<String> toolsUsed = new ArrayList<>();
            List<LeadStep> steps = new ArrayList<>();
            int totalInputTokens = 0;
            int totalOutputTokens = 0;

            for (int i = 0; i < capabilities.maxIterations(); i++) {
                String effectiveSystemPrompt = identity.systemPrompt();
                if (context.runMemory() != null && !context.runMemory().isEmpty()) {
                    effectiveSystemPrompt = effectiveSystemPrompt + "\n\n" + context.runMemory().buildSummary();
                }

                LlmRequest request = new LlmRequest(
                        effectiveSystemPrompt, messages, capabilities.toolDefinitions(),
                        identity.model(), capabilities.maxTokensPerRun(), capabilities.temperature());
                LlmResponse response = llmProvider.chat(request, apiKey);

                totalInputTokens += response.inputTokens();
                totalOutputTokens += response.outputTokens();

                if (response.content() != null && !response.content().isBlank()) {
                    steps.add(new LeadStep("reasoning", null, response.content(), i));
                }

                if (response.toolUse() != null) {
                    messages.add(LlmMessage.assistantToolUse(response.toolUse()));

                    steps.add(new LeadStep("tool_call", response.toolUse().name(), response.toolUse().input(), i));
                    publishActivity(context, AgentActivityEvent.Type.TOOL_CALL, response.toolUse().name());

                    String toolResultContent;
                    try {
                        SystemAgentTool tool = toolRegistry.getTool(response.toolUse().name())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "System-Tool nicht gefunden: " + response.toolUse().name()));
                        SystemToolResult result = tool.execute(toolContext, response.toolUse().input());
                        toolResultContent = result.success() ? result.data() : "Fehler: " + result.errorMessage();
                        toolsUsed.add(response.toolUse().name());
                    } catch (Exception e) {
                        log.error("System Lead tool execution error '{}': {}",
                                response.toolUse().name(), e.getMessage());
                        toolResultContent = "Fehler bei Tool-Ausführung: " + e.getMessage();
                    }

                    steps.add(new LeadStep("tool_result", response.toolUse().name(), toolResultContent, i));
                    publishActivity(context, AgentActivityEvent.Type.TOOL_RESULT, response.toolUse().name());

                    messages.add(LlmMessage.toolResult(
                            new LlmToolResult(response.toolUse().id(), toolResultContent)));
                } else {
                    log.info("System Lead {} completed task in {} iterations",
                            identity.name(), i + 1);
                    String content = response.content() != null ? response.content() : "";
                    return SystemAgentResult.completed(content, totalInputTokens, totalOutputTokens, toolsUsed, steps);
                }
            }

            log.warn("System Lead {} reached max iterations ({})", identity.name(), capabilities.maxIterations());
            return SystemAgentResult.maxIterations(
                    "System-Lead-Agent hat die maximale Anzahl an Iterationen erreicht.",
                    totalInputTokens, totalOutputTokens, toolsUsed);

        } catch (Exception e) {
            log.error("System LeadAgent error for '{}': {}", identity.name(), e.getMessage(), e);
            return SystemAgentResult.error("Fehler beim Ausführen des System-Lead-Agents: " + e.getMessage());
        }
    }

    private void publishActivity(SystemAgentContext context, AgentActivityEvent.Type type, String detail) {
        if (context.activityBus() == null) return;
        try {
            String truncated = detail != null && detail.length() > 120 ? detail.substring(0, 120) : detail;
            context.activityBus().publish(new AgentActivityEvent(
                    type, identity.instanceId(), null, identity.name(),
                    truncated, Instant.now()));
        } catch (Exception e) {
            log.debug("Failed to publish system lead activity event: {}", e.getMessage());
        }
    }
}
