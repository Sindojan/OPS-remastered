package com.owlsburg.ops.agentinfra.runtime;

import com.owlsburg.ops.agentinfra.llm.*;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.agentinfra.tools.ToolExecutionContext;
import com.owlsburg.ops.agentinfra.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                    context.tenantId(), identity.instanceId(), null);

            List<String> toolsUsed = new ArrayList<>();
            int totalInputTokens = 0;
            int totalOutputTokens = 0;

            for (int i = 0; i < capabilities.maxIterations(); i++) {
                LlmRequest request = new LlmRequest(
                        identity.systemPrompt(), messages, capabilities.toolDefinitions(),
                        identity.model(), capabilities.maxTokensPerRun());
                LlmResponse response = llmProvider.chat(request, apiKey);

                totalInputTokens += response.inputTokens();
                totalOutputTokens += response.outputTokens();

                if (response.toolUse() != null) {
                    messages.add(LlmMessage.assistantToolUse(response.toolUse()));

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

                    messages.add(LlmMessage.toolResult(
                            new LlmToolResult(response.toolUse().id(), toolResultContent)));

                    log.debug("Lead {} used tool: {} (iteration {})",
                            identity.name(), response.toolUse().name(), i + 1);
                } else {
                    log.info("Lead {} completed task in {} iterations", identity.name(), i + 1);
                    String content = response.content() != null ? response.content() : "";
                    return AgentResult.completed(content, totalInputTokens, totalOutputTokens, toolsUsed);
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
}
