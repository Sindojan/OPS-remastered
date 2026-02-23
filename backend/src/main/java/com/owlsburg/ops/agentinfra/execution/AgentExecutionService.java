package com.owlsburg.ops.agentinfra.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.*;
import com.owlsburg.ops.agentinfra.llm.*;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.agentinfra.tools.ToolExecutionContext;
import com.owlsburg.ops.agentinfra.tools.ToolResult;
import com.owlsburg.ops.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class AgentExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionService.class);
    private static final int MAX_ITERATIONS = 15;
    private static final int MAX_DELEGATION_DEPTH = 2;

    private final AgentRunService runService;
    private final AgentInstanceService instanceService;
    private final AgentTemplateService templateService;
    private final AgentToolRegistry toolRegistry;
    private final LlmConfigService llmConfigService;
    private final LlmProviderRegistry providerRegistry;
    private final AgentIncidentService incidentService;
    private final ObjectMapper objectMapper;

    public AgentExecutionService(AgentRunService runService,
                                  AgentInstanceService instanceService,
                                  AgentTemplateService templateService,
                                  AgentToolRegistry toolRegistry,
                                  LlmConfigService llmConfigService,
                                  LlmProviderRegistry providerRegistry,
                                  AgentIncidentService incidentService,
                                  ObjectMapper objectMapper) {
        this.runService = runService;
        this.instanceService = instanceService;
        this.templateService = templateService;
        this.toolRegistry = toolRegistry;
        this.llmConfigService = llmConfigService;
        this.providerRegistry = providerRegistry;
        this.incidentService = incidentService;
        this.objectMapper = objectMapper;
    }

    public void executeRun(UUID runId, int delegationDepth) {
        AgentRunEntity run = runService.findById(runId);
        AgentInstanceEntity instance = instanceService.findById(run.getInstanceId());
        AgentTemplateEntity template = templateService.findById(instance.getTemplateId());

        // Update status to RUNNING
        run.setStatus(AgentRunStatus.RUNNING);
        run.setStartedAt(java.time.Instant.now());

        // Budget check
        AgentRunService.BudgetCheckResult budget = runService.checkBudget(instance.getId());
        if (budget.tokensRemaining() <= 0) {
            runService.failRun(runId, "Token-Budget für heute erschöpft. Verbleibend: 0");
            return;
        }

        // Load allowed tools
        List<AgentTool> tools = toolRegistry.getToolsForInstance(template);

        // Determine model (instance config overrides template default)
        String model = resolveModel(instance, template);

        // Build system prompt
        String tenantId = TenantContext.getCurrentTenant();
        String systemPrompt = SystemPromptBuilder.build(template, tools, tenantId);

        // Get API key
        String apiKey;
        try {
            apiKey = llmConfigService.getDecryptedApiKey();
        } catch (LlmProviderException e) {
            runService.failRun(runId, "Kein LLM API-Key konfiguriert. Bitte unter Einstellungen hinterlegen.");
            return;
        }

        // Get LLM config for provider
        String providerName = llmConfigService.getConfig()
                .map(TenantLlmConfigEntity::getProvider)
                .orElse("anthropic");
        LlmProvider provider = providerRegistry.getProvider(providerName);

        // Build tool definitions for LLM
        List<LlmToolDefinition> toolDefinitions = new ArrayList<>();
        for (AgentTool tool : tools) {
            toolDefinitions.add(new LlmToolDefinition(tool.getName(), tool.getDescription(), tool.getInputSchema()));
        }
        // Add delegate_to_agent tool definition
        toolDefinitions.add(new LlmToolDefinition(
                "delegate_to_agent",
                "Delegiere eine Aufgabe an einen anderen Agenten.",
                "{\"type\":\"object\",\"properties\":{\"targetInstanceName\":{\"type\":\"string\",\"description\":\"Name der Ziel-Agent-Instanz\"},\"task\":{\"type\":\"string\",\"description\":\"Aufgabenbeschreibung für den Ziel-Agenten\"}},\"required\":[\"targetInstanceName\",\"task\"]}"
        ));

        // Initialize conversation
        List<LlmMessage> messages = new ArrayList<>();
        String inputContext = run.getInputContext() != null ? run.getInputContext() : "Starte deine Aufgabe.";
        messages.add(LlmMessage.user(inputContext));

        int totalInputTokens = 0;
        int totalOutputTokens = 0;
        String lastTextContent = null;

        // ReAct Loop
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            log.info("Run {}: Iteration {}/{}", runId, iteration + 1, MAX_ITERATIONS);

            long callStart = System.currentTimeMillis();
            LlmResponse response;
            try {
                LlmRequest request = new LlmRequest(
                        systemPrompt,
                        List.copyOf(messages),
                        toolDefinitions,
                        model,
                        template.getMaxTokensPerRun()
                );
                response = provider.chat(request, apiKey);
            } catch (LlmProviderException e) {
                log.error("LLM call failed for run {}: {}", runId, e.getMessage());
                runService.failRun(runId, "LLM-Fehler: " + e.getMessage());
                return;
            }
            long callDuration = System.currentTimeMillis() - callStart;

            totalInputTokens += response.inputTokens();
            totalOutputTokens += response.outputTokens();

            // Log LLM_CALL step
            runService.addStep(runId, AgentStepType.LLM_CALL, null,
                    null, truncateForStep(response.content()),
                    response.inputTokens() + response.outputTokens(), (int) callDuration);

            // Check stop reason
            if ("end_turn".equals(response.stopReason())) {
                lastTextContent = response.content();
                break;
            }

            if ("tool_use".equals(response.stopReason()) && response.toolUse() != null) {
                LlmToolUse toolUse = response.toolUse();

                // Add assistant message with tool use
                messages.add(LlmMessage.assistantToolUse(toolUse));

                String toolResultContent;
                long toolStart = System.currentTimeMillis();

                if ("delegate_to_agent".equals(toolUse.name())) {
                    // Handle delegation
                    toolResultContent = handleDelegation(toolUse, instance, delegationDepth);
                } else {
                    // Execute normal tool
                    Optional<AgentTool> toolOpt = toolRegistry.getTool(toolUse.name());
                    if (toolOpt.isEmpty()) {
                        toolResultContent = "Fehler: Tool '" + toolUse.name() + "' nicht gefunden.";
                    } else {
                        ToolExecutionContext ctx = new ToolExecutionContext(tenantId, instance.getId(), runId);
                        ToolResult result = toolOpt.get().execute(ctx, toolUse.input());
                        toolResultContent = result.success() ? result.data() : "Fehler: " + result.errorMessage();
                    }
                }
                long toolDuration = System.currentTimeMillis() - toolStart;

                // Log TOOL_CALL step
                runService.addStep(runId, AgentStepType.TOOL_CALL, toolUse.name(),
                        truncateForStep(toolUse.input()), truncateForStep(toolResultContent),
                        0, (int) toolDuration);

                // Add tool result as user message
                messages.add(LlmMessage.toolResult(new LlmToolResult(toolUse.id(), toolResultContent)));
            } else {
                // Unexpected stop reason or no tool use - take text content and finish
                lastTextContent = response.content();
                break;
            }
        }

        // Calculate cost
        int totalTokens = totalInputTokens + totalOutputTokens;
        BigDecimal cost = calculateCost(model, totalInputTokens, totalOutputTokens);

        // Complete the run
        String output = lastTextContent != null ? lastTextContent : "Keine Antwort generiert.";
        runService.completeRun(runId, output, totalTokens, cost);
        log.info("Run {} completed: {} tokens, ${}", runId, totalTokens, cost);
    }

    private String handleDelegation(LlmToolUse toolUse, AgentInstanceEntity parentInstance, int delegationDepth) {
        if (delegationDepth >= MAX_DELEGATION_DEPTH) {
            return "Fehler: Maximale Delegationstiefe (" + MAX_DELEGATION_DEPTH + ") erreicht.";
        }

        try {
            Map<String, String> input = objectMapper.readValue(toolUse.input(),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            String targetName = input.get("targetInstanceName");
            String task = input.get("task");

            if (targetName == null || task == null) {
                return "Fehler: targetInstanceName und task sind Pflichtfelder.";
            }

            // Find target instance by name
            List<AgentInstanceEntity> allInstances = instanceService.findAll();
            AgentInstanceEntity targetInstance = allInstances.stream()
                    .filter(i -> i.getName().equalsIgnoreCase(targetName))
                    .findFirst()
                    .orElse(null);

            if (targetInstance == null) {
                List<String> available = allInstances.stream()
                        .map(AgentInstanceEntity::getName)
                        .toList();
                return "Fehler: Agent-Instanz '" + targetName + "' nicht gefunden. Verfügbar: " + available;
            }

            if (targetInstance.getStatus() != AgentInstanceStatus.ACTIVE) {
                return "Fehler: Agent-Instanz '" + targetName + "' ist nicht aktiv (Status: " + targetInstance.getStatus() + ")";
            }

            // Create child run
            AgentRunEntity childRun = runService.startRun(
                    targetInstance.getId(),
                    TriggerType.EVENT,
                    "delegation:" + parentInstance.getId(),
                    task
            );

            // Execute child run synchronously
            executeRun(childRun.getId(), delegationDepth + 1);

            // Get result
            AgentRunEntity completedChild = runService.findById(childRun.getId());
            if (completedChild.getStatus() == AgentRunStatus.SUCCESS) {
                return completedChild.getOutput() != null ? completedChild.getOutput() : "Delegation erfolgreich, keine Ausgabe.";
            } else {
                return "Delegation fehlgeschlagen: " + (completedChild.getErrorMessage() != null
                        ? completedChild.getErrorMessage() : "Unbekannter Fehler");
            }
        } catch (Exception e) {
            log.error("Delegation failed: {}", e.getMessage(), e);
            return "Fehler bei Delegation: " + e.getMessage();
        }
    }

    private String resolveModel(AgentInstanceEntity instance, AgentTemplateEntity template) {
        // Check instance config for model override
        if (instance.getConfig() != null && !instance.getConfig().equals("{}")) {
            try {
                Map<String, Object> config = objectMapper.readValue(instance.getConfig(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                Object modelOverride = config.get("model");
                if (modelOverride != null && !modelOverride.toString().isBlank()) {
                    return modelOverride.toString();
                }
            } catch (Exception e) {
                log.warn("Failed to parse instance config for model override: {}", e.getMessage());
            }
        }

        // Fall back to LLM config default model, then template-implied default
        return llmConfigService.getConfig()
                .map(TenantLlmConfigEntity::getDefaultModel)
                .orElse("claude-sonnet-4-20250514");
    }

    private BigDecimal calculateCost(String model, int inputTokens, int outputTokens) {
        // Pricing per million tokens (approximate, as of 2025)
        BigDecimal inputPricePerMillion;
        BigDecimal outputPricePerMillion;

        if (model != null && model.contains("opus")) {
            inputPricePerMillion = new BigDecimal("15.00");
            outputPricePerMillion = new BigDecimal("75.00");
        } else if (model != null && model.contains("haiku")) {
            inputPricePerMillion = new BigDecimal("0.80");
            outputPricePerMillion = new BigDecimal("4.00");
        } else {
            // Sonnet default
            inputPricePerMillion = new BigDecimal("3.00");
            outputPricePerMillion = new BigDecimal("15.00");
        }

        BigDecimal inputCost = inputPricePerMillion
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputPricePerMillion
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
    }

    private String truncateForStep(String text) {
        if (text == null) return null;
        return text.length() > 4000 ? text.substring(0, 4000) + "..." : text;
    }
}
