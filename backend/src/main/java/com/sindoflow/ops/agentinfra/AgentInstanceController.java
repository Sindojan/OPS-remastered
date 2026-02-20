package com.sindoflow.ops.agentinfra;

import com.sindoflow.ops.agentinfra.dto.AgentInstanceResponse;
import com.sindoflow.ops.agentinfra.dto.CreateAgentInstanceRequest;
import com.sindoflow.ops.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent-instances")
public class AgentInstanceController {

    private final AgentInstanceService instanceService;

    public AgentInstanceController(AgentInstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgentInstanceResponse>>> list(
            @RequestParam(value = "templateId", required = false) UUID templateId,
            @RequestParam(value = "status", required = false) AgentInstanceStatus status,
            @RequestParam(value = "tenantId", required = false) String tenantId) {

        List<AgentInstanceEntity> instances;
        if (templateId != null) {
            instances = instanceService.findByTemplateId(templateId);
        } else if (status != null) {
            instances = instanceService.findByStatus(status);
        } else if (tenantId != null) {
            instances = instanceService.findByTenantId(tenantId);
        } else {
            instances = instanceService.findAll();
        }

        List<AgentInstanceResponse> response = instances.stream()
                .map(AgentInstanceResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> getById(@PathVariable UUID id) {
        AgentInstanceEntity instance = instanceService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(instance)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> create(
            @Valid @RequestBody CreateAgentInstanceRequest request) {

        AgentInstanceEntity entity = new AgentInstanceEntity();
        entity.setTemplateId(request.templateId());
        entity.setName(request.name());
        entity.setParentInstanceId(request.parentInstanceId());
        entity.setType(request.type());
        entity.setTenantId(request.tenantId());
        entity.setConfig(request.config() != null ? request.config() : "{}");

        AgentInstanceEntity created = instanceService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(AgentInstanceResponse.from(created), "Agent instance created"));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> activate(@PathVariable UUID id) {
        AgentInstanceEntity activated = instanceService.activate(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(activated), "Agent instance activated"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> deactivate(@PathVariable UUID id) {
        AgentInstanceEntity deactivated = instanceService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(deactivated), "Agent instance deactivated"));
    }

    @PatchMapping("/{id}/quarantine")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> quarantine(@PathVariable UUID id) {
        AgentInstanceEntity quarantined = instanceService.quarantine(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(quarantined), "Agent instance quarantined"));
    }

    @PatchMapping("/{id}/terminate")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> terminate(@PathVariable UUID id) {
        AgentInstanceEntity terminated = instanceService.terminate(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(terminated), "Agent instance terminated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        instanceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Agent instance deleted"));
    }
}
