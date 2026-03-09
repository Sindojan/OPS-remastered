package com.owlsburg.ops.common;

import com.owlsburg.ops.common.dto.ModuleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getAll() {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        List<ModuleEntity> allModules = moduleService.getAllModules();
        Set<String> enabledModules = moduleService.getEnabledModules(tenantId);

        List<ModuleResponse> responses = allModules.stream()
                .map(m -> new ModuleResponse(
                        m.getId(),
                        m.getLabel(),
                        m.getDescription(),
                        m.isCore(),
                        m.getDisplayOrder(),
                        m.isCore() || enabledModules.contains(m.getId())
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PutMapping("/{moduleId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ModuleResponse>> toggle(
            @PathVariable String moduleId,
            @RequestBody ToggleRequest request) {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        moduleService.toggleModule(tenantId, moduleId, request.enabled());

        ModuleEntity module = moduleService.getAllModules().stream()
                .filter(m -> m.getId().equals(moduleId))
                .findFirst()
                .orElseThrow();

        ModuleResponse response = new ModuleResponse(
                module.getId(),
                module.getLabel(),
                module.getDescription(),
                module.isCore(),
                module.getDisplayOrder(),
                request.enabled()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    public record ToggleRequest(boolean enabled) {}
}
