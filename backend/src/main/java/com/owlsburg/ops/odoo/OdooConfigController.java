package com.owlsburg.ops.odoo;

import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.odoo.dto.OdooConfigResponse;
import com.owlsburg.ops.odoo.dto.OdooConnectionTestResponse;
import com.owlsburg.ops.odoo.dto.SaveOdooConfigRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/odoo")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class OdooConfigController {

    private final OdooConfigService odooConfigService;

    public OdooConfigController(OdooConfigService odooConfigService) {
        this.odooConfigService = odooConfigService;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<OdooConfigResponse>> getConfig() {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        return odooConfigService.getConfig(tenantId)
                .map(config -> ResponseEntity.ok(ApiResponse.ok(config)))
                .orElse(ResponseEntity.ok(ApiResponse.ok(
                        new OdooConfigResponse(null, null, "19.0", false, "UNCONFIGURED", null)
                )));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<OdooConfigResponse>> saveConfig(
            @Valid @RequestBody SaveOdooConfigRequest request) {
        OdooConfigResponse response = odooConfigService.saveConfig(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/test-connection")
    public ResponseEntity<ApiResponse<OdooConnectionTestResponse>> testConnection() {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        OdooConnectionTestResponse response = odooConfigService.testConnection(tenantId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
