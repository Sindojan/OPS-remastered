package com.sindoflow.ops.tenant;

import com.sindoflow.ops.config.FlywayConfig;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final JdbcTemplate jdbcTemplate;
    private final FlywayConfig flywayConfig;

    public TenantService(TenantRepository tenantRepository, JdbcTemplate jdbcTemplate, FlywayConfig flywayConfig) {
        this.tenantRepository = tenantRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.flywayConfig = flywayConfig;
    }

    @Transactional
    public TenantEntity createTenant(String name, String slug) {
        if (tenantRepository.existsByTenantId(slug)) {
            throw new IllegalArgumentException("Tenant already exists: " + slug);
        }

        String schemaName = "tenant_" + slug;

        // Create tenant record in public schema
        TenantEntity tenant = new TenantEntity();
        tenant.setTenantId(slug);
        tenant.setName(name);
        tenant.setSchemaName(schemaName);
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        // Create the PostgreSQL schema
        log.info("Creating schema: {}", schemaName);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + sanitizeSchemaName(schemaName));

        // Run Flyway migrations on the new schema
        flywayConfig.migrateTenantSchema(schemaName);

        log.info("Tenant created successfully: {} (schema: {})", name, schemaName);
        return tenant;
    }

    @Transactional(readOnly = true)
    public TenantEntity findById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + id));
    }

    @Transactional(readOnly = true)
    public TenantEntity findByTenantId(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
    }

    @Transactional(readOnly = true)
    public List<TenantEntity> findAllActive() {
        return tenantRepository.findByActiveTrue();
    }

    @Transactional
    public TenantEntity deactivate(UUID id) {
        TenantEntity tenant = findById(id);
        tenant.setActive(false);
        log.info("Deactivated tenant: {}", tenant.getTenantId());
        return tenantRepository.save(tenant);
    }

    @Transactional
    public TenantEntity activate(UUID id) {
        TenantEntity tenant = findById(id);
        tenant.setActive(true);
        log.info("Activated tenant: {}", tenant.getTenantId());
        return tenantRepository.save(tenant);
    }

    private String sanitizeSchemaName(String schemaName) {
        // Only allow alphanumeric and underscore
        if (!schemaName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
        return schemaName;
    }
}
