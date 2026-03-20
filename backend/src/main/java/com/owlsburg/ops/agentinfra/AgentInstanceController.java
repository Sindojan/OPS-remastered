package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.AgentInstanceResponse;
import com.owlsburg.ops.agentinfra.dto.AgentRunResponse;
import com.owlsburg.ops.agentinfra.dto.AgentRunStepResponse;
import com.owlsburg.ops.agentinfra.dto.CreateAgentInstanceRequest;
import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.common.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent-instances")
public class AgentInstanceController {

    private static final Set<String> VALID_MODELS = Set.of(
            "claude-opus-4-6",
            "claude-sonnet-4-6",
            "claude-haiku-4-5-20251001"
    );

    private final AgentInstanceService instanceService;
    private final AgentRunService agentRunService;

    public AgentInstanceController(AgentInstanceService instanceService,
                                   AgentRunService agentRunService) {
        this.instanceService = instanceService;
        this.agentRunService = agentRunService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgentInstanceResponse>>> list(
            @RequestParam(value = "templateId", required = false) UUID templateId,
            @RequestParam(value = "status", required = false) AgentInstanceStatus status) {

        List<AgentInstanceEntity> instances;
        if (templateId != null) {
            instances = instanceService.findByTemplateId(templateId);
        } else if (status != null) {
            instances = instanceService.findByStatus(status);
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
        AgentInstanceEntity instance = instanceService.findByIdSecure(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(instance)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> activate(@PathVariable UUID id) {
        AgentInstanceEntity activated = instanceService.activate(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(activated), "Agent instance activated"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> deactivate(@PathVariable UUID id) {
        AgentInstanceEntity deactivated = instanceService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(deactivated), "Agent instance deactivated"));
    }

    @PatchMapping("/{id}/quarantine")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> quarantine(@PathVariable UUID id) {
        AgentInstanceEntity quarantined = instanceService.quarantine(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(quarantined), "Agent instance quarantined"));
    }

    @PatchMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> terminate(@PathVariable UUID id) {
        AgentInstanceEntity terminated = instanceService.terminate(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(terminated), "Agent instance terminated"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> updateInstance(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> body) {
        String systemPrompt = body.get("systemPrompt");
        if (systemPrompt == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("systemPrompt field is required"));
        }
        AgentInstanceEntity updated = instanceService.updateSystemPrompt(id, systemPrompt);
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(updated), "System-Prompt aktualisiert"));
    }

    @PatchMapping("/{id}/model")
    public ResponseEntity<ApiResponse<AgentInstanceResponse>> updateModel(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> body) {
        String model = body.get("model");
        if (model == null || model.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("model field is required"));
        }
        if (!VALID_MODELS.contains(model)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Ungültiges Modell: " + model
                            + ". Erlaubt: " + VALID_MODELS));
        }
        AgentInstanceEntity instance = instanceService.findByIdSecure(id);
        // Merge model into existing config JSON
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> config = instance.getConfig() != null && !instance.getConfig().equals("{}")
                    ? mapper.readValue(instance.getConfig(),
                        mapper.getTypeFactory().constructMapType(java.util.Map.class, String.class, Object.class))
                    : new java.util.HashMap<>();
            config.put("model", model);
            instance.setConfig(mapper.writeValueAsString(config));
        } catch (Exception e) {
            instance.setConfig("{\"model\":\"" + model + "\"}");
        }
        AgentInstanceEntity saved = instanceService.updateConfig(id, instance.getConfig());
        return ResponseEntity.ok(ApiResponse.ok(AgentInstanceResponse.from(saved), "Model updated"));
    }

    @GetMapping("/{id}/last-run")
    public ResponseEntity<ApiResponse<AgentRunResponse>> getLastRun(@PathVariable UUID id) {
        AgentInstanceEntity instance = instanceService.findByIdSecure(id);
        if (instance.getLastRunId() == null) {
            return ResponseEntity.ok(ApiResponse.ok(null, "Kein letzter Run vorhanden"));
        }
        AgentRunEntity run = agentRunService.findById(instance.getLastRunId());
        List<AgentRunStepResponse> steps = agentRunService.findStepsByRunId(run.getId())
                .stream().map(AgentRunStepResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(AgentRunResponse.from(run, steps)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        instanceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Agent instance deleted"));
    }
}
