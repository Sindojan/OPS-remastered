package com.owlsburg.ops.agentinfra.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentTemplateEntity;
import com.owlsburg.ops.agentinfra.AgentTemplateRepository;
import com.owlsburg.ops.agentinfra.llm.*;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.agentinfra.tools.ToolExecutionContext;
import com.owlsburg.ops.agentinfra.tools.ToolResult;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.tenant.TenantRepository;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LeadAgentRunner {

    private static final Logger log = LoggerFactory.getLogger(LeadAgentRunner.class);
    private static final int MAX_ITERATIONS = 5;

    private final AgentToolRegistry toolRegistry;
    private final LlmConfigService llmConfigService;
    private final LlmProviderRegistry providerRegistry;
    private final AgentTemplateRepository templateRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public LeadAgentRunner(@Lazy AgentToolRegistry toolRegistry,
                           LlmConfigService llmConfigService,
                           LlmProviderRegistry providerRegistry,
                           AgentTemplateRepository templateRepository,
                           TenantRepository tenantRepository,
                           ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.llmConfigService = llmConfigService;
        this.providerRegistry = providerRegistry;
        this.templateRepository = templateRepository;
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper;
    }

    public String runLead(AgentInstanceEntity leadInstance, String task, String tenantId) {
        try {
            // 1. Load template
            AgentTemplateEntity template = templateRepository.findById(leadInstance.getTemplateId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Template nicht gefunden für Lead: " + leadInstance.getName()));

            // 2. Build tools from template's allowedTools
            List<AgentTool> tools = toolRegistry.getToolsForInstance(template);
            List<LlmToolDefinition> toolDefs = tools.stream()
                    .map(t -> new LlmToolDefinition(t.getName(), t.getDescription(), t.getInputSchema()))
                    .toList();

            // 3. Build system prompt
            String basePrompt = (leadInstance.getCustomSystemPrompt() != null
                    && !leadInstance.getCustomSystemPrompt().isBlank())
                    ? leadInstance.getCustomSystemPrompt()
                    : template.getBasePrompt();
            String tenantName = resolveTenantName(tenantId);
            String systemPrompt = buildSystemPrompt(basePrompt, tenantName);

            // 4. Get API key, model (per-instance → tenant default), provider
            String apiKey = llmConfigService.getDecryptedApiKey();
            String model = resolveModel(leadInstance);
            String providerName = llmConfigService.getConfig()
                    .map(c -> c.getProvider())
                    .orElse("anthropic");
            LlmProvider provider = providerRegistry.getProvider(providerName);

            // 5. Sync ReAct loop
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.user(task));

            ToolExecutionContext toolContext = new ToolExecutionContext(tenantId, leadInstance.getId(), null);

            for (int i = 0; i < MAX_ITERATIONS; i++) {
                LlmRequest request = new LlmRequest(systemPrompt, messages, toolDefs, model, 4096);
                LlmResponse response = provider.chat(request, apiKey);

                if (response.toolUse() != null) {
                    // Add assistant tool_use message
                    messages.add(LlmMessage.assistantToolUse(response.toolUse()));

                    // Execute tool
                    String toolResultContent;
                    try {
                        AgentTool tool = toolRegistry.getTool(response.toolUse().name())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Tool nicht gefunden: " + response.toolUse().name()));
                        ToolResult result = tool.execute(toolContext, response.toolUse().input());
                        toolResultContent = result.success() ? result.data() : "Fehler: " + result.errorMessage();
                    } catch (Exception e) {
                        log.error("Lead tool execution error '{}': {}",
                                response.toolUse().name(), e.getMessage());
                        toolResultContent = "Fehler bei Tool-Ausführung: " + e.getMessage();
                    }

                    // Add tool result
                    messages.add(LlmMessage.toolResult(
                            new LlmToolResult(response.toolUse().id(), toolResultContent)));

                    log.debug("Lead {} used tool: {} (iteration {})",
                            leadInstance.getName(), response.toolUse().name(), i + 1);
                } else {
                    // end_turn – return text
                    log.info("Lead {} completed task in {} iterations",
                            leadInstance.getName(), i + 1);
                    return response.content() != null ? response.content() : "";
                }
            }

            // Max iterations reached – return last content if any
            log.warn("Lead {} reached max iterations ({})", leadInstance.getName(), MAX_ITERATIONS);
            return "Lead-Agent hat die maximale Anzahl an Iterationen erreicht.";

        } catch (Exception e) {
            log.error("LeadAgentRunner error for '{}': {}", leadInstance.getName(), e.getMessage(), e);
            return "Fehler beim Ausführen des Lead-Agents: " + e.getMessage();
        }
    }

    private String resolveModel(AgentInstanceEntity leadInstance) {
        if (leadInstance.getConfig() != null && !leadInstance.getConfig().equals("{}")) {
            try {
                com.fasterxml.jackson.databind.JsonNode configNode = objectMapper.readTree(leadInstance.getConfig());
                if (configNode.has("model") && !configNode.get("model").asText().isBlank()) {
                    return configNode.get("model").asText();
                }
            } catch (Exception e) {
                log.warn("Failed to parse lead instance config for model: {}", e.getMessage());
            }
        }
        return llmConfigService.getConfig()
                .map(c -> c.getDefaultModel())
                .orElse("claude-sonnet-4-6");
    }

    private String buildSystemPrompt(String basePrompt, String tenantName) {
        String base = (basePrompt != null && !basePrompt.isBlank())
                ? basePrompt : "Du bist ein spezialisierter Lead-Agent.";
        String resolved = base.replace("{{TENANT_NAME}}", tenantName);
        return resolved + "\n\n## Kontext\n- Datum: " + LocalDate.now() + "\n";
    }

    private String resolveTenantName(String tenantId) {
        if (tenantId != null) {
            try {
                return tenantRepository.findById(UUID.fromString(tenantId))
                        .map(t -> t.getName())
                        .orElse("Owlsburg OPS");
            } catch (IllegalArgumentException e) {
                return "Owlsburg OPS";
            }
        }
        return "Owlsburg OPS";
    }
}
