package com.owlsburg.ops.systemagent.runtime;

import com.owlsburg.ops.agentinfra.llm.*;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemAgentToolRegistry;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class SystemSubAgent implements SystemAgent {

    private static final Logger log = LoggerFactory.getLogger(SystemSubAgent.class);

    private final SystemAgentIdentity identity;
    private final SystemAgentCapabilities capabilities;
    private final LlmProvider llmProvider;
    private final String apiKey;
    private final SystemAgentToolRegistry toolRegistry;

    public SystemSubAgent(SystemAgentIdentity identity, SystemAgentCapabilities capabilities,
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
                    identity.instanceId(), null, null, context.runMemory());

            List<String> toolsUsed = new ArrayList<>();
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

                if (response.toolUse() != null) {
                    messages.add(LlmMessage.assistantToolUse(response.toolUse()));

                    String toolResultContent;
                    try {
                        SystemAgentTool tool = toolRegistry.getTool(response.toolUse().name())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "System-Tool nicht gefunden: " + response.toolUse().name()));
                        SystemToolResult result = tool.execute(toolContext, response.toolUse().input());
                        toolResultContent = result.success() ? result.data() : "Fehler: " + result.errorMessage();
                        toolsUsed.add(response.toolUse().name());
                    } catch (Exception e) {
                        log.error("System SubAgent tool execution error '{}': {}",
                                response.toolUse().name(), e.getMessage());
                        toolResultContent = "Fehler bei Tool-Ausführung: " + e.getMessage();
                    }

                    messages.add(LlmMessage.toolResult(
                            new LlmToolResult(response.toolUse().id(), toolResultContent)));
                } else {
                    String content = response.content() != null ? response.content() : "";
                    return SystemAgentResult.completed(content, totalInputTokens, totalOutputTokens, toolsUsed);
                }
            }

            return SystemAgentResult.maxIterations(
                    "System-Sub-Agent hat die maximale Anzahl an Iterationen erreicht.",
                    totalInputTokens, totalOutputTokens, toolsUsed);

        } catch (Exception e) {
            log.error("System SubAgent error for '{}': {}", identity.name(), e.getMessage(), e);
            return SystemAgentResult.error("Fehler beim Ausführen des System-Sub-Agents: " + e.getMessage());
        }
    }
}
