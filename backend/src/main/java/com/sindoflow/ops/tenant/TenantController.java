package com.sindoflow.ops.tenant;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.tenant.dto.TenantCreateRequest;
import com.sindoflow.ops.tenant.dto.TenantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tenants")
@PreAuthorize("hasRole('ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> create(@Valid @RequestBody TenantCreateRequest request) {
        TenantEntity tenant = tenantService.createTenant(request.name(), request.slug());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(TenantResponse.from(tenant), "Tenant created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getAll() {
        List<TenantResponse> tenants = tenantService.findAllActive().stream()
                .map(TenantResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(tenants));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> getById(@PathVariable UUID id) {
        TenantResponse tenant = TenantResponse.from(tenantService.findById(id));
        return ResponseEntity.ok(ApiResponse.ok(tenant));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<TenantResponse>> deactivate(@PathVariable UUID id) {
        TenantEntity tenant = tenantService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(TenantResponse.from(tenant), "Tenant deactivated"));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<TenantResponse>> activate(@PathVariable UUID id) {
        TenantEntity tenant = tenantService.activate(id);
        return ResponseEntity.ok(ApiResponse.ok(TenantResponse.from(tenant), "Tenant activated"));
    }
}
