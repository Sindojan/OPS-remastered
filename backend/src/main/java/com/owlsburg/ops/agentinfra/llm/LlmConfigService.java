package com.owlsburg.ops.agentinfra.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.common.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class LlmConfigService {

    private static final Logger log = LoggerFactory.getLogger(LlmConfigService.class);

    private final TenantLlmConfigRepository configRepository;
    private final EncryptionService encryptionService;
    private final LlmProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    public LlmConfigService(TenantLlmConfigRepository configRepository,
                            EncryptionService encryptionService,
                            LlmProviderRegistry providerRegistry,
                            ObjectMapper objectMapper) {
        this.configRepository = configRepository;
        this.encryptionService = encryptionService;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<TenantLlmConfigEntity> getConfig() {
        return configRepository.findFirstByOrderByCreatedAtAsc();
    }

    @Transactional
    public TenantLlmConfigEntity saveConfig(String provider, String apiKey, String defaultModel) {
        String providerName = (provider != null && !provider.isBlank()) ? provider : "anthropic";
        String model = (defaultModel != null && !defaultModel.isBlank()) ? defaultModel : "claude-sonnet-4-20250514";

        // Validate provider exists
        providerRegistry.getProvider(providerName);

        String encryptedKey = encryptionService.encrypt(apiKey);

        TenantLlmConfigEntity config = configRepository.findFirstByOrderByCreatedAtAsc()
                .orElse(new TenantLlmConfigEntity());

        config.setProvider(providerName);
        config.setApiKeyEnc(encryptedKey);
        config.setDefaultModel(model);

        TenantLlmConfigEntity saved = configRepository.save(config);
        log.info("Saved LLM config for provider: {}, model: {}", providerName, model);
        return saved;
    }

    @Transactional(readOnly = true)
    public String getDecryptedApiKey() {
        TenantLlmConfigEntity config = configRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new LlmProviderException("LLM not configured. Please set up an API key in settings."));

        if (config.getApiKeyEnc() == null || config.getApiKeyEnc().isBlank()) {
            throw new LlmProviderException("LLM API key not configured.");
        }

        return encryptionService.decrypt(config.getApiKeyEnc());
    }

    @Transactional(readOnly = true)
    public List<String> listModels() {
        Optional<TenantLlmConfigEntity> configOpt = configRepository.findFirstByOrderByCreatedAtAsc();
        if (configOpt.isEmpty() || configOpt.get().getApiKeyEnc() == null) {
            return Collections.emptyList();
        }

        TenantLlmConfigEntity config = configOpt.get();
        String apiKey = encryptionService.decrypt(config.getApiKeyEnc());
        LlmProvider provider = providerRegistry.getProvider(config.getProvider());
        return provider.listModels(apiKey);
    }

    @Transactional
    public TenantLlmConfigEntity saveConfigWithSettings(String provider, String apiKey, String defaultModel,
                                                         Double temperature, Integer maxTokensDefault) {
        TenantLlmConfigEntity saved = saveConfig(provider, apiKey, defaultModel);

        // Build settings JSONB
        try {
            ObjectNode settingsNode = objectMapper.createObjectNode();
            if (temperature != null) {
                settingsNode.put("temperature", temperature);
            }
            if (maxTokensDefault != null) {
                settingsNode.put("maxTokensDefault", maxTokensDefault);
            }
            saved.setSettings(objectMapper.writeValueAsString(settingsNode));
            configRepository.save(saved);
        } catch (Exception e) {
            log.warn("Failed to save LLM settings: {}", e.getMessage());
        }

        return saved;
    }

    public Double getTemperature(TenantLlmConfigEntity config) {
        try {
            var node = objectMapper.readTree(config.getSettings() != null ? config.getSettings() : "{}");
            if (node.has("temperature")) return node.get("temperature").asDouble();
        } catch (Exception ignored) {}
        return null;
    }

    public Integer getMaxTokensDefault(TenantLlmConfigEntity config) {
        try {
            var node = objectMapper.readTree(config.getSettings() != null ? config.getSettings() : "{}");
            if (node.has("maxTokensDefault")) return node.get("maxTokensDefault").asInt();
        } catch (Exception ignored) {}
        return null;
    }

    public List<String> listModelsWithKey(String provider, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Collections.emptyList();
        }
        String providerName = (provider != null && !provider.isBlank()) ? provider : "anthropic";
        LlmProvider llmProvider = providerRegistry.getProvider(providerName);
        return llmProvider.listModels(apiKey);
    }
}
