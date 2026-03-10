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

public final class SubAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(SubAgent.class);

    private final AgentIdentity identity;
    private final AgentCapabilities capabilities;
    private final LlmProvider llmProvider;
    private final String apiKey;
    private final AgentToolRegistry toolRegistry;

    public SubAgent(AgentIdentity identity, AgentCapabilities capabilities,
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
                    context.tenantId(), identity.instanceId(), null, null, context.runMemory());

            List<String> toolsUsed = new ArrayList<>();
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
                        log.error("SubAgent tool execution error '{}': {}",
                                response.toolUse().name(), e.getMessage());
                        toolResultContent = "Fehler bei Tool-Ausführung: " + e.getMessage();
                    }

                    messages.add(LlmMessage.toolResult(
                            new LlmToolResult(response.toolUse().id(), toolResultContent)));
                } else {
                    log.info("SubAgent {} completed task in {} iterations", identity.name(), i + 1);
                    String content = response.content() != null ? response.content() : "";
                    return AgentResult.completed(content, totalInputTokens, totalOutputTokens, toolsUsed);
                }
            }

            log.warn("SubAgent {} reached max iterations ({})", identity.name(), capabilities.maxIterations());
            return AgentResult.maxIterations(
                    "Sub-Agent hat die maximale Anzahl an Iterationen erreicht.",
                    totalInputTokens, totalOutputTokens, toolsUsed);

        } catch (Exception e) {
            log.error("SubAgent error for '{}': {}", identity.name(), e.getMessage(), e);
            return AgentResult.error("Fehler beim Ausführen des Sub-Agents: " + e.getMessage());
        }
    }
}
