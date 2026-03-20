package com.owlsburg.ops.odoo;

import com.owlsburg.ops.common.EncryptionService;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.odoo.dto.OdooConfigResponse;
import com.owlsburg.ops.odoo.dto.OdooConnectionTestResponse;
import com.owlsburg.ops.odoo.dto.SaveOdooConfigRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class OdooConfigService {

    private static final Logger log = LoggerFactory.getLogger(OdooConfigService.class);

    private final OdooConfigRepository configRepository;
    private final EncryptionService encryptionService;
    private final OdooClient odooClient;

    public OdooConfigService(OdooConfigRepository configRepository,
                             EncryptionService encryptionService,
                             OdooClient odooClient) {
        this.configRepository = configRepository;
        this.encryptionService = encryptionService;
        this.odooClient = odooClient;
    }

    @Transactional(readOnly = true)
    public Optional<OdooConfigResponse> getConfig(UUID tenantId) {
        return configRepository.findByTenantId(tenantId)
                .map(this::toResponse);
    }

    @Transactional
    public OdooConfigResponse saveConfig(SaveOdooConfigRequest request) {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());

        OdooConfigEntity config = configRepository.findByTenantId(tenantId)
                .orElse(new OdooConfigEntity());

        config.setBaseUrl(request.baseUrl().trim());
        config.setDatabaseName(request.databaseName().trim());
        config.setApiKeyEnc(encryptionService.encrypt(request.apiKey()));
        config.setOdooVersion(request.odooVersion() != null ? request.odooVersion() : "19.0");
        config.setConnectionStatus("PENDING");

        OdooConfigEntity saved = configRepository.save(config);
        log.info("Saved Odoo config for tenant {}", tenantId);
        return toResponse(saved);
    }

    @Transactional
    public OdooConnectionTestResponse testConnection(UUID tenantId) {
        OdooConfigEntity config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new OdooApiException("Odoo ist nicht konfiguriert. Bitte zuerst Verbindungsdaten speichern."));

        String apiKey = encryptionService.decrypt(config.getApiKeyEnc());

        try {
            odooClient.testConnection(config.getBaseUrl(), config.getDatabaseName(), apiKey);

            config.setConnectionStatus("CONNECTED");
            config.setLastConnectedAt(Instant.now());
            configRepository.save(config);

            log.info("Odoo connection test successful for tenant {}", tenantId);
            return new OdooConnectionTestResponse(true, config.getOdooVersion(), "Verbindung erfolgreich");
        } catch (OdooApiException e) {
            config.setConnectionStatus("ERROR");
            configRepository.save(config);

            log.warn("Odoo connection test failed for tenant {}: {}", tenantId, e.getMessage());
            return new OdooConnectionTestResponse(false, null, "Verbindung fehlgeschlagen: " + e.getMessage());
        }
    }

    /**
     * Internal: Get decrypted connection details for tools.
     */
    @Transactional(readOnly = true)
    public OdooConnectionDetails getConnectionDetails(UUID tenantId) {
        OdooConfigEntity config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new OdooApiException("Odoo ist nicht konfiguriert für diesen Tenant."));

        return new OdooConnectionDetails(
                config.getBaseUrl(),
                config.getDatabaseName(),
                encryptionService.decrypt(config.getApiKeyEnc())
        );
    }

    private OdooConfigResponse toResponse(OdooConfigEntity entity) {
        return new OdooConfigResponse(
                entity.getBaseUrl(),
                entity.getDatabaseName(),
                entity.getOdooVersion(),
                entity.getApiKeyEnc() != null && !entity.getApiKeyEnc().isBlank(),
                entity.getConnectionStatus(),
                entity.getLastConnectedAt()
        );
    }

    public record OdooConnectionDetails(String baseUrl, String database, String apiKey) {}
}
