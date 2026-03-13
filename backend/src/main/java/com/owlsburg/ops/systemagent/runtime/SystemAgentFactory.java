package com.owlsburg.ops.systemagent.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentInstanceType;
import com.owlsburg.ops.agentinfra.llm.LlmProvider;
import com.owlsburg.ops.agentinfra.llm.LlmProviderRegistry;
import com.owlsburg.ops.agentinfra.llm.LlmToolDefinition;
import com.owlsburg.ops.systemagent.*;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemAgentToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SystemAgentFactory {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentFactory.class);

    private final SystemAgentInstanceRepository instanceRepository;
    private final SystemAgentTemplateRepository templateRepository;
    private final SystemAgentToolRegistry toolRegistry;
    private final SystemAgentMemoryService memoryService;
    private final SystemLlmConfigService llmConfigService;
    private final LlmProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    public SystemAgentFactory(SystemAgentInstanceRepository instanceRepository,
                              SystemAgentTemplateRepository templateRepository,
                              @Lazy SystemAgentToolRegistry toolRegistry,
                              SystemAgentMemoryService memoryService,
                              SystemLlmConfigService llmConfigService,
                              LlmProviderRegistry providerRegistry,
                              ObjectMapper objectMapper) {
        this.instanceRepository = instanceRepository;
        this.templateRepository = templateRepository;
        this.toolRegistry = toolRegistry;
        this.memoryService = memoryService;
        this.llmConfigService = llmConfigService;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper;
    }

    public SystemAgent createAgent(UUID instanceId) {
        SystemAgentInstanceEntity instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalStateException("System-Agent-Instanz nicht gefunden: " + instanceId));

        SystemAgentTemplateEntity template = templateRepository.findById(instance.getTemplateId())
                .orElseThrow(() -> new IllegalStateException("System-Template nicht gefunden für: " + instance.getName()));

        return buildAgent(instance, template);
    }

    private SystemAgent buildAgent(SystemAgentInstanceEntity instance, SystemAgentTemplateEntity template) {
        String role = template.getRole();

        // Resolve system prompt
        String basePrompt = (instance.getCustomSystemPrompt() != null && !instance.getCustomSystemPrompt().isBlank())
                ? instance.getCustomSystemPrompt()
                : template.getBasePrompt();
        String systemPrompt = buildSystemPrompt(basePrompt);

        // Inject memory section
        String memorySection = memoryService.buildMemorySection(instance.getId());
        systemPrompt = systemPrompt + memorySection;

        // Resolve model
        String model = resolveModel(instance);

        // Parse allowed tools
        List<String> allowedToolNames;
        if (instance.getAllowedToolsOverride() != null) {
            try {
                allowedToolNames = objectMapper.readValue(instance.getAllowedToolsOverride(),
                        new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse allowedToolsOverride for system instance {}: {}", instance.getId(), e.getMessage());
                allowedToolNames = parseAllowedTools(template);
            }
        } else {
            allowedToolNames = parseAllowedTools(template);
        }

        // Build identity (no tenantId)
        SystemAgentIdentity identity = new SystemAgentIdentity(
                instance.getId(), instance.getTemplateId(),
                instance.getName(), role, instance.getType(),
                instance.getParentInstanceId(), systemPrompt, model, allowedToolNames);

        // Build tools and capabilities
        List<SystemAgentTool> tools = toolRegistry.getToolsByNames(allowedToolNames);
        List<LlmToolDefinition> toolDefs = tools.stream()
                .map(t -> new LlmToolDefinition(t.getName(), t.getDescription(), t.getInputSchema()))
                .toList();

        Double temperature = resolveTemperature();
        int maxTokensPerRun = resolveMaxTokensPerRun(instance, template);

        // Get API key
        String apiKey = llmConfigService.getDecryptedApiKey();

        if ("system_ceo".equals(role)) {
            SystemAgentCapabilities capabilities = new SystemAgentCapabilities(
                    tools, toolDefs, true, false, false, 10, maxTokensPerRun, temperature);
            return new SystemCeoAgent(identity, capabilities, apiKey, toolRegistry, objectMapper);
        } else {
            boolean isLead = role.endsWith("_lead");
            SystemAgentCapabilities capabilities = new SystemAgentCapabilities(
                    tools, toolDefs, false, isLead, isLead, 5, maxTokensPerRun, temperature);

            String providerName = llmConfigService.getConfig()
                    .map(SystemLlmConfigEntity::getProvider).orElse("ANTHROPIC");
            LlmProvider provider = providerRegistry.getProvider(providerName.toLowerCase());

            return new SystemLeadAgent(identity, capabilities, provider, apiKey, toolRegistry);
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
                            log.warn("Failed to parse system LLM settings for temperature: {}", e.getMessage());
                        }
                        return null;
                    })
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private int resolveMaxTokensPerRun(SystemAgentInstanceEntity instance, SystemAgentTemplateEntity template) {
        if (instance.getConfig() != null && !"{}".equals(instance.getConfig())) {
            try {
                JsonNode configNode = objectMapper.readTree(instance.getConfig());
                if (configNode.has("maxTokensPerRun")) {
                    return configNode.get("maxTokensPerRun").asInt();
                }
            } catch (Exception e) {
                log.warn("Failed to parse system instance config for maxTokensPerRun: {}", e.getMessage());
            }
        }
        return template.getMaxTokensPerRun();
    }

    private String resolveModel(SystemAgentInstanceEntity instance) {
        if (instance.getConfig() != null && !"{}".equals(instance.getConfig())) {
            try {
                JsonNode configNode = objectMapper.readTree(instance.getConfig());
                if (configNode.has("model") && !configNode.get("model").asText().isBlank()) {
                    return configNode.get("model").asText();
                }
            } catch (Exception e) {
                log.warn("Failed to parse system instance config for model: {}", e.getMessage());
            }
        }
        return llmConfigService.getConfig()
                .map(SystemLlmConfigEntity::getDefaultModel)
                .orElse("claude-sonnet-4-6");
    }

    private String buildSystemPrompt(String basePrompt) {
        String base = (basePrompt != null && !basePrompt.isBlank())
                ? basePrompt : "Du bist ein hilfreicher System-Assistent von Owlsburg OPS.";
        return base + "\n\n## Kontext\n- Datum: " + LocalDate.now()
                + "\n- Ebene: System (Plattform-übergreifend, kein einzelner Tenant)\n";
    }

    private List<String> parseAllowedTools(SystemAgentTemplateEntity template) {
        try {
            return objectMapper.readValue(template.getAllowedTools(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to parse system allowedTools: {}", e.getMessage());
            return List.of();
        }
    }
}
