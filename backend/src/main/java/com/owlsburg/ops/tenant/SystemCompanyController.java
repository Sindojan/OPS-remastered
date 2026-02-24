package com.owlsburg.ops.tenant;

import com.owlsburg.ops.auth.UserEntity;
import com.owlsburg.ops.auth.UserService;
import com.owlsburg.ops.auth.dto.UserResponse;
import com.owlsburg.ops.common.ApiResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/system/companies")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemCompanyController {

    private static final Logger log = LoggerFactory.getLogger(SystemCompanyController.class);

    private final SystemCompanyService companyService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public SystemCompanyController(SystemCompanyService companyService, UserService userService,
                                   PasswordEncoder passwordEncoder) {
        this.companyService = companyService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
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
}
