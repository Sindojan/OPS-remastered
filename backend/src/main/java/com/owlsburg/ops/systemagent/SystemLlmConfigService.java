package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.common.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SystemLlmConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemLlmConfigService.class);

    private final SystemLlmConfigRepository configRepository;
    private final EncryptionService encryptionService;

    public SystemLlmConfigService(SystemLlmConfigRepository configRepository,
                                   EncryptionService encryptionService) {
        this.configRepository = configRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public Optional<SystemLlmConfigEntity> getConfig() {
        return configRepository.findAll().stream().findFirst();
    }

    @Transactional
    public SystemLlmConfigEntity saveConfig(String provider, String apiKey, String defaultModel,
                                             String settings) {
        SystemLlmConfigEntity config = configRepository.findAll().stream().findFirst()
                .orElseGet(SystemLlmConfigEntity::new);

        config.setProvider(provider);
        config.setApiKeyEnc(encryptionService.encrypt(apiKey));
        config.setDefaultModel(defaultModel);
        if (settings != null) {
            config.setSettings(settings);
        }

        log.info("Saved system LLM config (provider={}, model={})", provider, defaultModel);
        return configRepository.save(config);
    }

    public String getDecryptedApiKey() {
        return getConfig()
                .map(c -> encryptionService.decrypt(c.getApiKeyEnc()))
                .orElseThrow(() -> new IllegalStateException(
                        "System-LLM-Konfiguration nicht gefunden. Bitte zuerst einen API-Key konfigurieren."));
    }
}
