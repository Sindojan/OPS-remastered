package com.owlsburg.ops.tenant;

import com.owlsburg.ops.tenant.dto.TenantConfigUpdateRequest;
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

    @Transactional
    public TenantEntity updateConfig(UUID tenantId, TenantConfigUpdateRequest request) {
        TenantEntity tenant = findById(tenantId);

        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.contactEmail() != null) {
            tenant.setContactEmail(request.contactEmail());
        }
        if (request.contactPhone() != null) {
            tenant.setContactPhone(request.contactPhone());
        }
        if (request.address() != null) {
            tenant.setAddress(request.address());
        }
        if (request.city() != null) {
            tenant.setCity(request.city());
        }
        if (request.postalCode() != null) {
            tenant.setPostalCode(request.postalCode());
        }
        if (request.country() != null) {
            tenant.setCountry(request.country());
        }
        if (request.website() != null) {
            tenant.setWebsite(request.website());
        }
        if (request.vatId() != null) {
            tenant.setVatId(request.vatId());
        }

        log.info("Updated config for tenant: {}", tenantId);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public TenantEntity updateLogoUrl(UUID tenantId, String logoUrl) {
        TenantEntity tenant = findById(tenantId);
        tenant.setLogoUrl(logoUrl);
        log.info("Updated logo URL for tenant: {}", tenantId);
        return tenantRepository.save(tenant);
    }
}
