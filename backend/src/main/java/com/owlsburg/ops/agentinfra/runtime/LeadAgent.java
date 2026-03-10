package com.owlsburg.ops.agentinfra.runtime;

import com.owlsburg.ops.agentinfra.llm.*;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.agentinfra.tools.ToolExecutionContext;
import com.owlsburg.ops.agentinfra.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class LeadAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(LeadAgent.class);

    private final AgentIdentity identity;
    private final AgentCapabilities capabilities;
    private final LlmProvider llmProvider;
    private final String apiKey;
    private final AgentToolRegistry toolRegistry;

    public LeadAgent(AgentIdentity identity, AgentCapabilities capabilities,
                     LlmProvider llmProvider, String apiKey, AgentToolRegistry toolRegistry) {
        this.identity = identity;
        this.capabilities = capabilities;
        this.llmProvider = llmProvider;
        this.apiKey = apiKey;
        this.toolRegistry = toolRegistry;
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
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.user(task));

            ToolExecutionContext toolContext = new ToolExecutionContext(
                    context.tenantId(), identity.instanceId(), null, context.activityBus(), context.runMemory());

            List<String> toolsUsed = new ArrayList<>();
            List<LeadStep> steps = new ArrayList<>();
            int totalInputTokens = 0;
            int totalOutputTokens = 0;

            for (int i = 0; i < capabilities.maxIterations(); i++) {
                // Inject RunMemory summary into system prompt if available
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

                // Capture reasoning if present
                if (response.content() != null && !response.content().isBlank()) {
                    steps.add(new LeadStep("reasoning", null, response.content(), i));
                }

                if (response.toolUse() != null) {
                    messages.add(LlmMessage.assistantToolUse(response.toolUse()));

                    // Capture tool call
                    steps.add(new LeadStep("tool_call", response.toolUse().name(), response.toolUse().input(), i));
                    publishActivity(context, AgentActivityEvent.Type.TOOL_CALL, response.toolUse().name());

                    String toolResultContent;
                    try {
                        AgentTool tool = toolRegistry.getTool(response.toolUse().name())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Tool nicht gefunden: " + response.toolUse().name()));
                        ToolResult result = tool.execute(toolContext, response.toolUse().input());
                        toolResultContent = result.success() ? result.data() : "Fehler: " + result.errorMessage();
                        toolsUsed.add(response.toolUse().name());
                    } catch (Exception e) {
                        log.error("Lead tool execution error '{}': {}",
                                response.toolUse().name(), e.getMessage());
                        toolResultContent = "Fehler bei Tool-Ausführung: " + e.getMessage();
                    }

                    // Capture tool result
                    steps.add(new LeadStep("tool_result", response.toolUse().name(), toolResultContent, i));
                    publishActivity(context, AgentActivityEvent.Type.TOOL_RESULT, response.toolUse().name());

                    messages.add(LlmMessage.toolResult(
                            new LlmToolResult(response.toolUse().id(), toolResultContent)));

                    log.debug("Lead {} used tool: {} (iteration {})",
                            identity.name(), response.toolUse().name(), i + 1);
                } else {
                    log.info("Lead {} completed task in {} iterations ({} steps)",
                            identity.name(), i + 1, steps.size());
                    String content = response.content() != null ? response.content() : "";
                    return AgentResult.completed(content, totalInputTokens, totalOutputTokens, toolsUsed, steps);
                }
            }

            log.warn("Lead {} reached max iterations ({})", identity.name(), capabilities.maxIterations());
            return AgentResult.maxIterations(
                    "Lead-Agent hat die maximale Anzahl an Iterationen erreicht.",
                    totalInputTokens, totalOutputTokens, toolsUsed);

        } catch (Exception e) {
            log.error("LeadAgent error for '{}': {}", identity.name(), e.getMessage(), e);
            return AgentResult.error("Fehler beim Ausführen des Lead-Agents: " + e.getMessage());
        }
    }

    private void publishActivity(AgentContext context, AgentActivityEvent.Type type, String detail) {
        if (context.activityBus() == null) return;
        try {
            String truncated = detail != null && detail.length() > 120 ? detail.substring(0, 120) : detail;
            context.activityBus().publish(context.tenantId(), new AgentActivityEvent(
                    type, identity.instanceId(), null, identity.name(),
                    truncated, Instant.now()));
        } catch (Exception e) {
            log.debug("Failed to publish lead activity event: {}", e.getMessage());
        }
    }
}
