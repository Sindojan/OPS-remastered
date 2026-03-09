package com.owlsburg.ops.tenant;

import com.owlsburg.ops.auth.UserEntity;
import com.owlsburg.ops.auth.UserService;
import com.owlsburg.ops.auth.dto.UserResponse;
import com.owlsburg.ops.common.*;
import com.owlsburg.ops.common.dto.ModuleResponse;
import com.owlsburg.ops.tenant.dto.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/companies")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemCompanyController {

    private static final Logger log = LoggerFactory.getLogger(SystemCompanyController.class);

    private final SystemCompanyService companyService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ModuleService moduleService;

    public SystemCompanyController(SystemCompanyService companyService, UserService userService,
                                   PasswordEncoder passwordEncoder, ModuleService moduleService) {
        this.companyService = companyService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.moduleService = moduleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> findAll() {
        List<TenantResponse> companies = companyService.findAll().stream()
                .map(TenantResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(companies));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyCreateResponse>> create(@Valid @RequestBody CompanyCreateRequest request) {
        String password = companyService.generatePassword(request);
        CompanyCreateRequest requestWithPassword = new CompanyCreateRequest(
                request.name(), request.slug(), request.plan(),
                request.adminEmail(), request.adminFirstName(), request.adminLastName(),
                password
        );
        TenantEntity tenant = companyService.create(requestWithPassword, passwordEncoder);

        CompanyCreateResponse response = new CompanyCreateResponse(
                tenant.getId(), tenant.getName(), tenant.getSlug(),
                tenant.getPlan(), request.adminEmail(), password
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> findById(@PathVariable UUID id) {
        TenantEntity tenant = companyService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(TenantResponse.from(tenant)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> update(@PathVariable UUID id,
                                                               @Valid @RequestBody CompanyUpdateRequest request) {
        TenantEntity tenant = companyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(TenantResponse.from(tenant)));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<TenantResponse>> suspend(@PathVariable UUID id,
                                                                @Valid @RequestBody SuspendRequest request) {
        TenantEntity tenant = companyService.suspend(id, request.reason());
        return ResponseEntity.ok(ApiResponse.ok(TenantResponse.from(tenant)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<TenantResponse>> activate(@PathVariable UUID id) {
        TenantEntity tenant = companyService.activate(id);
        return ResponseEntity.ok(ApiResponse.ok(TenantResponse.from(tenant)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        companyService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Company deleted"));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<CompanyStatsResponse>> getStats(@PathVariable UUID id) {
        CompanyStatsResponse stats = companyService.getStats(id);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/{id}/admins")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAdmins(@PathVariable UUID id) {
        List<UserResponse> admins = companyService.getAdmins(id).stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(admins));
    }

    @PostMapping("/{id}/admins/{userId}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetAdminPassword(@PathVariable UUID id,
                                                                   @PathVariable UUID userId) {
        UserEntity user = userService.findById(userId);
        if (!user.getTenantId().equals(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("User does not belong to this company"));
        }
        String newPassword = UUID.randomUUID().toString().substring(0, 12);
        userService.resetPassword(userId, newPassword);
        log.info("Reset password for admin {} in company {}", userId, id);
        return ResponseEntity.ok(ApiResponse.ok(newPassword, "Password reset successfully"));
    }

    // ─── Module Management ─────────────────────────────────

    @GetMapping("/{id}/modules")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getModules(@PathVariable UUID id) {
        return withTenantContext(id, () -> {
            List<ModuleEntity> allModules = moduleService.getAllModules();
            Set<String> enabled = moduleService.getEnabledModules(id);

            List<ModuleResponse> responses = allModules.stream()
                    .map(m -> new ModuleResponse(
                            m.getId(), m.getLabel(), m.getDescription(),
                            m.isCore(), m.getDisplayOrder(),
                            m.isCore() || enabled.contains(m.getId())
                    ))
                    .toList();

            return ResponseEntity.ok(ApiResponse.ok(responses));
        });
    }

    @PutMapping("/{id}/modules/{moduleId}/toggle")
    public ResponseEntity<ApiResponse<ModuleResponse>> toggleModule(
            @PathVariable UUID id,
            @PathVariable String moduleId,
            @RequestBody ModuleToggleRequest request) {
        return withTenantContext(id, () -> {
            moduleService.toggleModule(id, moduleId, request.enabled());

            ModuleEntity module = moduleService.getAllModules().stream()
                    .filter(m -> m.getId().equals(moduleId))
                    .findFirst()
                    .orElseThrow();

            ModuleResponse response = new ModuleResponse(
                    module.getId(), module.getLabel(), module.getDescription(),
                    module.isCore(), module.getDisplayOrder(), request.enabled()
            );

            return ResponseEntity.ok(ApiResponse.ok(response));
        });
    }

    public record ModuleToggleRequest(boolean enabled) {}

    /**
     * Executes the given operation with the tenant context temporarily set.
     * Required because SYSTEM_ADMIN has no tenant context, but RLS on tenant_modules
     * requires it.
     */
    private <T> T withTenantContext(UUID tenantId, java.util.function.Supplier<T> operation) {
        try {
            TenantContext.setCurrentTenant(tenantId.toString());
            return operation.get();
        } finally {
            TenantContext.clear();
        }
    }
}
