package com.owlsburg.ops.tenant;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public TenantEntity createTenant(String name) {
        TenantEntity tenant = new TenantEntity();
        tenant.setName(name);
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);
        log.info("Tenant created: {} (id: {})", name, tenant.getId());
        return tenant;
    }

    @Transactional(readOnly = true)
    public TenantEntity findById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<TenantEntity> findAllActive() {
        return tenantRepository.findByActiveTrue();
    }

    @Transactional
    public TenantEntity deactivate(UUID id) {
        TenantEntity tenant = findById(id);
        tenant.setActive(false);
        log.info("Deactivated tenant: {}", id);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public TenantEntity activate(UUID id) {
        TenantEntity tenant = findById(id);
        tenant.setActive(true);
        log.info("Activated tenant: {}", id);
        return tenantRepository.save(tenant);
    }
}
