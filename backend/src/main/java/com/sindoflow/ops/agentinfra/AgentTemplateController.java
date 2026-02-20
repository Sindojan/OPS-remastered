package com.sindoflow.ops.agentinfra;

import com.sindoflow.ops.agentinfra.dto.AgentTemplateResponse;
import com.sindoflow.ops.agentinfra.dto.CreateAgentTemplateRequest;
import com.sindoflow.ops.agentinfra.dto.UpdateAgentTemplateRequest;
import com.sindoflow.ops.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent-templates")
public class AgentTemplateController {

    private final AgentTemplateService templateService;

    public AgentTemplateController(AgentTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgentTemplateResponse>>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "role", required = false) String role) {

        List<AgentTemplateEntity> templates;
        if (status != null) {
            templates = templateService.findByStatus(status);
        } else if (role != null) {
            templates = templateService.findByRole(role);
        } else {
            templates = templateService.findAll();
        }

        List<AgentTemplateResponse> response = templates.stream()
                .map(AgentTemplateResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentTemplateResponse>> getById(@PathVariable UUID id) {
        AgentTemplateEntity template = templateService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentTemplateResponse.from(template)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AgentTemplateResponse>> create(
            @Valid @RequestBody CreateAgentTemplateRequest request) {

        AgentTemplateEntity entity = new AgentTemplateEntity();
        entity.setName(request.name());
        entity.setRole(request.role());
        entity.setDescription(request.description());
        entity.setBasePrompt(request.basePrompt());
        entity.setAllowedTools(request.allowedTools() != null ? request.allowedTools() : "[]");
        entity.setTriggerTypes(request.triggerTypes() != null ? request.triggerTypes() : "[]");
        entity.setMaxTokensPerRun(request.maxTokensPerRun());
        entity.setDailyTokenBudget(request.dailyTokenBudget());
        entity.setStatus(request.status() != null ? request.status() : "ACTIVE");
        entity.setVersion(request.version() > 0 ? request.version() : 1);

        AgentTemplateEntity created = templateService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(AgentTemplateResponse.from(created), "Agent template created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentTemplateResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAgentTemplateRequest request) {

        AgentTemplateEntity entity = new AgentTemplateEntity();
        entity.setName(request.name());
        entity.setRole(request.role());
        entity.setDescription(request.description());
        entity.setBasePrompt(request.basePrompt());
        entity.setAllowedTools(request.allowedTools() != null ? request.allowedTools() : "[]");
        entity.setTriggerTypes(request.triggerTypes() != null ? request.triggerTypes() : "[]");
        entity.setMaxTokensPerRun(request.maxTokensPerRun());
        entity.setDailyTokenBudget(request.dailyTokenBudget());
        entity.setStatus(request.status() != null ? request.status() : "ACTIVE");
        entity.setVersion(request.version());

        AgentTemplateEntity updated = templateService.update(id, entity);
        return ResponseEntity.ok(ApiResponse.ok(AgentTemplateResponse.from(updated), "Agent template updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Agent template deleted"));
    }
}
