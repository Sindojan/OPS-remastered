package com.owlsburg.ops.agentinfra.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.*;
import com.owlsburg.ops.agentinfra.llm.*;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryService;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgentFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentFactory.class);

    private final AgentInstanceRepository instanceRepository;
    private final AgentTemplateRepository templateRepository;
    private final AgentToolRegistry toolRegistry;
    private final AgentMemoryService memoryService;
    private final LlmConfigService llmConfigService;
    private final LlmProviderRegistry providerRegistry;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public AgentFactory(AgentInstanceRepository instanceRepository,
                        AgentTemplateRepository templateRepository,
                        @Lazy AgentToolRegistry toolRegistry,
                        AgentMemoryService memoryService,
                        LlmConfigService llmConfigService,
                        LlmProviderRegistry providerRegistry,
                        TenantRepository tenantRepository,
                        ObjectMapper objectMapper) {
        this.instanceRepository = instanceRepository;
        this.templateRepository = templateRepository;
        this.toolRegistry = toolRegistry;
        this.memoryService = memoryService;
        this.llmConfigService = llmConfigService;
        this.providerRegistry = providerRegistry;
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates an Agent object from a persisted instance.
     * The Agent is request-scoped: built from DB state, used, then discarded.
     */
    public Agent createAgent(UUID instanceId, String tenantId) {
        UUID tenantUuid = UUID.fromString(tenantId);

        AgentInstanceEntity instance = instanceRepository.findByIdAndTenantId(instanceId, tenantUuid)
                .orElseThrow(() -> new IllegalStateException("Agent-Instanz nicht gefunden: " + instanceId));

        AgentTemplateEntity template = templateRepository.findById(instance.getTemplateId())
                .orElseThrow(() -> new IllegalStateException("Template nicht gefunden für: " + instance.getName()));

        return buildAgent(instance, template, tenantId);
    }

    /**
     * Spawns an ephemeral Sub-Agent from a parent Lead.
     */
    public SubAgent spawnSubAgent(Agent parent, String task, List<String> toolSubset,
                                   String name, String tenantId) {
        // Validate tool subset
        List<String> parentTools = parent.identity().allowedToolNames();
        for (String tool : toolSubset) {
            if (!parentTools.contains(tool)) {
                throw new IllegalArgumentException(
                        "Tool '" + tool + "' ist nicht im Toolset des Parent-Agents");
            }
        }

        // Create ephemeral instance in DB
        AgentInstanceEntity subInstance = new AgentInstanceEntity();
        subInstance.setTemplateId(parent.identity().templateId());
        subInstance.setName(name != null ? name : "Sub-Agent");
        subInstance.setParentInstanceId(parent.identity().instanceId());
        subInstance.setType(AgentInstanceType.EPHEMERAL);
        subInstance.setStatus(AgentInstanceStatus.ACTIVE);
        subInstance.setConfig("{}");

        AgentInstanceEntity saved = instanceRepository.save(subInstance);
        log.info("Spawned sub-agent '{}' (id: {}) for parent '{}'",
                name, saved.getId(), parent.identity().name());

        // Build identity
        String systemPrompt = buildSubAgentPrompt(name, task, toolSubset, tenantId);
        String model = resolveModel(saved);

        AgentIdentity identity = new AgentIdentity(
                saved.getId(), saved.getTemplateId(), UUID.fromString(tenantId),
                name, "sub_agent", AgentInstanceType.EPHEMERAL,
                parent.identity().instanceId(), systemPrompt, model, toolSubset);

        // Build capabilities
        List<AgentTool> tools = toolSubset.stream()
                .map(toolRegistry::getTool)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        List<LlmToolDefinition> toolDefs = tools.stream()
                .map(t -> new LlmToolDefinition(t.getName(), t.getDescription(), t.getInputSchema()))
                .toList();

        Double temperature = resolveTemperature();
        AgentCapabilities capabilities = new AgentCapabilities(
                tools, toolDefs, false, false, false, 3, 2048, temperature);

        // Get LLM provider
        String apiKey = llmConfigService.getDecryptedApiKey();
        String providerName = llmConfigService.getConfig()
                .map(c -> c.getProvider()).orElse("anthropic");
        LlmProvider provider = providerRegistry.getProvider(providerName);

        return new SubAgent(identity, capabilities, provider, apiKey, toolRegistry);
    }

    private Agent buildAgent(AgentInstanceEntity instance, AgentTemplateEntity template, String tenantId) {
        String role = template.getRole();

        // Resolve system prompt
        String basePrompt = (instance.getCustomSystemPrompt() != null && !instance.getCustomSystemPrompt().isBlank())
                ? instance.getCustomSystemPrompt()
                : template.getBasePrompt();
        String tenantName = resolveTenantName(tenantId);
        String systemPrompt = buildSystemPrompt(basePrompt, tenantName);

        // Inject memory section
        String memorySection = memoryService.buildMemorySection(instance.getId());
        systemPrompt = systemPrompt + memorySection;

        // Resolve model
        String model = resolveModel(instance);

        // Parse allowed tools – instance override takes priority over template
        List<String> allowedToolNames;
        if (instance.getAllowedToolsOverride() != null) {
            try {
                allowedToolNames = objectMapper.readValue(instance.getAllowedToolsOverride(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse allowedToolsOverride for instance {}: {}", instance.getId(), e.getMessage());
                allowedToolNames = parseAllowedTools(template);
            }
        } else {
            allowedToolNames = parseAllowedTools(template);
        }

        // Build identity
        AgentIdentity identity = new AgentIdentity(
                instance.getId(), instance.getTemplateId(), UUID.fromString(tenantId),
                instance.getName(), role, instance.getType(),
                instance.getParentInstanceId(), systemPrompt, model, allowedToolNames);

        // Build tools and capabilities – use instance override if present
        UUID tenantUuid = UUID.fromString(tenantId);
        List<AgentTool> tools;
        if (instance.getAllowedToolsOverride() != null) {
            tools = toolRegistry.getToolsByNames(allowedToolNames, tenantUuid);
        } else {
            tools = toolRegistry.getToolsForInstance(template, tenantUuid);
        }
        List<LlmToolDefinition> toolDefs = tools.stream()
                .map(t -> new LlmToolDefinition(t.getName(), t.getDescription(), t.getInputSchema()))
                .toList();

        // Resolve temperature from LLM settings
        Double temperature = resolveTemperature();

        // Resolve maxTokensPerRun – instance config override > template default
        int maxTokensPerRun = resolveMaxTokensPerRun(instance, template);

        // Get API key
        String apiKey = llmConfigService.getDecryptedApiKey();

        switch (role) {
            case "ceo" -> {
                AgentCapabilities capabilities = new AgentCapabilities(
                        tools, toolDefs, true, false, false, 10, maxTokensPerRun, temperature);
                return new CeoAgent(identity, capabilities, apiKey, toolRegistry, objectMapper);
            }
            default -> {
                // All _lead roles and any future roles → LeadAgent
                boolean isLead = role.endsWith("_lead");
                AgentCapabilities capabilities = new AgentCapabilities(
                        tools, toolDefs, false, isLead, isLead, 5, maxTokensPerRun, temperature);

                String providerName = llmConfigService.getConfig()
                        .map(c -> c.getProvider()).orElse("anthropic");
                LlmProvider provider = providerRegistry.getProvider(providerName);

                return new LeadAgent(identity, capabilities, provider, apiKey, toolRegistry);
            }
        }
    }

    private Double resolveTemperature() {
        try {
            return llmConfigService.getConfig()
                    .map(config -> {
                        try {
                            JsonNode settingsNode = objectMapper.readTree(
                                    config.getSettings() != null ? config.getSettings() : "{}");
                            if (settingsNode.has("temperature")) {
                                return settingsNode.get("temperature").asDouble();
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse LLM settings for temperature: {}", e.getMessage());
                        }
                        return null;
                    })
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private int resolveMaxTokensPerRun(AgentInstanceEntity instance, AgentTemplateEntity template) {
        if (instance.getConfig() != null && !"{}".equals(instance.getConfig())) {
            try {
                JsonNode configNode = objectMapper.readTree(instance.getConfig());
                if (configNode.has("maxTokensPerRun")) {
                    return configNode.get("maxTokensPerRun").asInt();
                }
            } catch (Exception e) {
                log.warn("Failed to parse instance config for maxTokensPerRun: {}", e.getMessage());
            }
        }
        return template.getMaxTokensPerRun();
    }

    private String resolveModel(AgentInstanceEntity instance) {
        if (instance.getConfig() != null && !"{}".equals(instance.getConfig())) {
            try {
                JsonNode configNode = objectMapper.readTree(instance.getConfig());
                if (configNode.has("model") && !configNode.get("model").asText().isBlank()) {
                    return configNode.get("model").asText();
                }
            } catch (Exception e) {
                log.warn("Failed to parse instance config for model: {}", e.getMessage());
            }
        }
        return llmConfigService.getConfig()
                .map(c -> c.getDefaultModel())
                .orElse("claude-sonnet-4-6");
    }

    private String buildSystemPrompt(String basePrompt, String tenantName) {
        String base = (basePrompt != null && !basePrompt.isBlank())
                ? basePrompt : "Du bist ein hilfreicher Assistent.";
        String resolved = base.replace("{{TENANT_NAME}}", tenantName);
        return resolved + "\n\n## Kontext\n- Datum: " + LocalDate.now() + "\n";
    }

    private String buildSubAgentPrompt(String name, String task, List<String> tools, String tenantId) {
        String tenantName = resolveTenantName(tenantId);
        StringBuilder sb = new StringBuilder();
        sb.append("Du bist ein spezialisierter Sub-Agent namens '").append(name).append("' von ").append(tenantName).append(".\n");
        sb.append("Du wurdest von einem Lead-Agent erstellt für eine spezifische Aufgabe.\n\n");
        sb.append("## Aufgabe\n").append(task).append("\n\n");
        sb.append("## Verfügbare Tools\n");
        for (String tool : tools) {
            sb.append("- ").append(tool).append("\n");
        }
        sb.append("\n## Regeln\n");
        sb.append("- Bearbeite nur die zugewiesene Aufgabe\n");
        sb.append("- Antworte kompakt und präzise auf Deutsch\n");
        sb.append("- Nutze die verfügbaren Tools\n");
        sb.append("\n## Kontext\n- Datum: ").append(LocalDate.now()).append("\n");
        return sb.toString();
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

    private List<String> parseAllowedTools(AgentTemplateEntity template) {
        try {
            return objectMapper.readValue(template.getAllowedTools(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to parse allowedTools: {}", e.getMessage());
            return List.of();
        }
    }
}
