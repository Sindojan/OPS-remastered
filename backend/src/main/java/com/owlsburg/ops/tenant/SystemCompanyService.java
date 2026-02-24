package com.owlsburg.ops.tenant;

import com.owlsburg.ops.auth.Role;
import com.owlsburg.ops.auth.UserEntity;
import com.owlsburg.ops.auth.UserRepository;
import com.owlsburg.ops.tenant.dto.CompanyCreateRequest;
import com.owlsburg.ops.tenant.dto.CompanyStatsResponse;
import com.owlsburg.ops.tenant.dto.CompanyUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SystemCompanyService {

    private static final Logger log = LoggerFactory.getLogger(SystemCompanyService.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public SystemCompanyService(TenantRepository tenantRepository, UserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TenantEntity> findAll() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TenantEntity findById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found: " + id));
    }

    @Transactional
    public TenantEntity create(CompanyCreateRequest request, PasswordEncoder encoder) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Slug already in use: " + request.slug());
        }
        if (userRepository.existsByEmail(request.adminEmail())) {
            throw new IllegalArgumentException("Admin email already in use: " + request.adminEmail());
        }

        TenantEntity tenant = new TenantEntity();
        tenant.setName(request.name());
        tenant.setSlug(request.slug());
        tenant.setPlan(request.plan() != null ? request.plan() : "BASIC");
        tenant.setStatus("ACTIVE");
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        String password = request.adminPassword() != null
                ? request.adminPassword()
                : UUID.randomUUID().toString().substring(0, 12);

        UserEntity admin = new UserEntity();
        admin.setTenantId(tenant.getId());
        admin.setEmail(request.adminEmail());
        admin.setPasswordHash(encoder.encode(password));
        admin.setFirstName(request.adminFirstName());
        admin.setLastName(request.adminLastName());
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        log.info("Created company '{}' (slug: {}) with admin {}", tenant.getName(), tenant.getSlug(), request.adminEmail());
        return tenant;
    }

    public String generatePassword(CompanyCreateRequest request) {
        return request.adminPassword() != null
                ? request.adminPassword()
                : UUID.randomUUID().toString().substring(0, 12);
    }

    @Transactional
    public TenantEntity update(UUID id, CompanyUpdateRequest request) {
        TenantEntity tenant = findById(id);
        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.plan() != null) {
            tenant.setPlan(request.plan());
        }
        log.info("Updated company: {}", id);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public TenantEntity suspend(UUID id, String reason) {
        TenantEntity tenant = findById(id);
        tenant.setStatus("SUSPENDED");
        tenant.setActive(false);
        tenant.setSuspendedAt(Instant.now());
        tenant.setSuspendReason(reason);
        log.info("Suspended company: {} (reason: {})", id, reason);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public TenantEntity activate(UUID id) {
        TenantEntity tenant = findById(id);
        tenant.setStatus("ACTIVE");
        tenant.setActive(true);
        tenant.setSuspendedAt(null);
        tenant.setSuspendReason(null);
        log.info("Activated company: {}", id);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void delete(UUID id) {
        TenantEntity tenant = findById(id);
        long userCount = userRepository.countByTenantId(id);
        if (userCount > 1) {
            throw new IllegalStateException("Cannot delete company with " + userCount + " users. Remove users first.");
        }
        tenant.setStatus("DELETED");
        tenant.setActive(false);
        tenantRepository.save(tenant);
        log.info("Soft-deleted company: {}", id);
    }

    @Transactional(readOnly = true)
    public CompanyStatsResponse getStats(UUID id) {
        findById(id); // ensure exists
        long userCount = userRepository.countByTenantId(id);
        return new CompanyStatsResponse(userCount, 0L, 0L, null);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAdmins(UUID tenantId) {
        return userRepository.findByTenantIdAndRole(tenantId, Role.ADMIN);
    }
}
