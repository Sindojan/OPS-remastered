package com.owlsburg.ops.tenant;

import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.documents.DocumentStorageService;
import com.owlsburg.ops.tenant.dto.TenantConfigResponse;
import com.owlsburg.ops.tenant.dto.TenantConfigUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant")
public class TenantConfigController {

    private static final Logger log = LoggerFactory.getLogger(TenantConfigController.class);

    private final TenantService tenantService;
    private final DocumentStorageService storageService;

    public TenantConfigController(TenantService tenantService,
                                  DocumentStorageService storageService) {
        this.tenantService = tenantService;
        this.storageService = storageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TenantConfigResponse>> get() {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        TenantEntity tenant = tenantService.findById(tenantId);
        return ResponseEntity.ok(ApiResponse.ok(TenantConfigResponse.from(tenant)));
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TenantConfigResponse>> update(@RequestBody TenantConfigUpdateRequest request) {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        TenantEntity tenant = tenantService.updateConfig(tenantId, request);
        return ResponseEntity.ok(ApiResponse.ok(TenantConfigResponse.from(tenant)));
    }

    @PostMapping("/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TenantConfigResponse>> uploadLogo(
            @RequestParam("file") MultipartFile file) {

        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());

        String originalFilename = file.getOriginalFilename();
        String ext = "png";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
        }

        String objectKey = "tenant-logos/" + tenantId + "/logo." + ext;

        try {
            storageService.upload(objectKey, file.getInputStream(), file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded logo file", e);
        }

        TenantEntity tenant = tenantService.updateLogoUrl(tenantId, objectKey);
        log.info("Uploaded logo for tenant {}: {}", tenantId, objectKey);
        return ResponseEntity.ok(ApiResponse.ok(TenantConfigResponse.from(tenant), "Logo uploaded"));
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TenantConfigResponse>> deleteLogo() {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        TenantEntity tenant = tenantService.findById(tenantId);

        if (tenant.getLogoUrl() != null) {
            try {
                storageService.delete(tenant.getLogoUrl());
            } catch (Exception e) {
                log.warn("Failed to delete logo from storage: {}", tenant.getLogoUrl(), e);
            }
        }

        tenant = tenantService.updateLogoUrl(tenantId, null);
        log.info("Deleted logo for tenant {}", tenantId);
        return ResponseEntity.ok(ApiResponse.ok(TenantConfigResponse.from(tenant), "Logo deleted"));
    }
}
