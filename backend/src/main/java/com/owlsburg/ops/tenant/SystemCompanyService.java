package com.owlsburg.ops.tenant;

import com.owlsburg.ops.agentinfra.AgentRunRepository;
import com.owlsburg.ops.auth.Role;
import com.owlsburg.ops.auth.UserEntity;
import com.owlsburg.ops.auth.UserRepository;
import com.owlsburg.ops.common.ModuleRepository;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.common.TenantModuleEntity;
import com.owlsburg.ops.common.TenantModuleRepository;
import com.owlsburg.ops.tenant.dto.CompanyCreateRequest;
import com.owlsburg.ops.tenant.dto.CompanyStatsResponse;
import com.owlsburg.ops.tenant.dto.CompanyUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SystemCompanyService {

    private static final Logger log = LoggerFactory.getLogger(SystemCompanyService.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AgentRunRepository agentRunRepository;
    private final ModuleRepository moduleRepository;
    private final TenantModuleRepository tenantModuleRepository;

    public SystemCompanyService(TenantRepository tenantRepository, UserRepository userRepository,
                                AgentRunRepository agentRunRepository, ModuleRepository moduleRepository,
                                TenantModuleRepository tenantModuleRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.agentRunRepository = agentRunRepository;
        this.moduleRepository = moduleRepository;
        this.tenantModuleRepository = tenantModuleRepository;
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

        TenantEntity tenantEntity = new TenantEntity();
        tenantEntity.setName(request.name());
        tenantEntity.setSlug(request.slug());
        tenantEntity.setPlan(request.plan() != null ? request.plan() : "BASIC");
        tenantEntity.setStatus("ACTIVE");
        tenantEntity.setActive(true);
        final TenantEntity tenant = tenantRepository.save(tenantEntity);

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

        // Initialize all modules for the new tenant (all enabled by default)
        // Temporarily set tenant context for RLS on tenant_modules table
        try {
            TenantContext.setCurrentTenant(tenant.getId().toString());
            moduleRepository.findAll().forEach(module -> {
                TenantModuleEntity tm = new TenantModuleEntity();
                tm.setTenantId(tenant.getId());
                tm.setModuleId(module.getId());
                tm.setEnabled(true);
                tm.setEnabledAt(Instant.now());
                tenantModuleRepository.save(tm);
            });
        } finally {
            TenantContext.clear();
        }

        log.info("Created company '{}' (slug: {}) with admin {} and {} modules",
                tenant.getName(), tenant.getSlug(), request.adminEmail(),
                moduleRepository.count());
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
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        long runs = agentRunRepository.countByTenantSince(id, since);
        long tokens = agentRunRepository.sumTokensByTenantSince(id, since);
        BigDecimal cost = agentRunRepository.sumCostByTenantSince(id, since);
        Instant lastActive = agentRunRepository.findLastActiveByTenant(id);
        return new CompanyStatsResponse(userCount, runs, 0L, lastActive, tokens, cost);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAdmins(UUID tenantId) {
        return userRepository.findByTenantIdAndRole(tenantId, Role.ADMIN);
    }
}
