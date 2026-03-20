package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.AgentTemplateResponse;
import com.owlsburg.ops.agentinfra.dto.AvailableToolResponse;
import com.owlsburg.ops.agentinfra.dto.CreateAgentTemplateRequest;
import com.owlsburg.ops.agentinfra.dto.UpdateAgentTemplateRequest;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent-templates")
public class AgentTemplateController {

    private final AgentTemplateService templateService;
    private final AgentToolRegistry toolRegistry;

    public AgentTemplateController(AgentTemplateService templateService,
                                   AgentToolRegistry toolRegistry) {
        this.templateService = templateService;
        this.toolRegistry = toolRegistry;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
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

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AgentTemplateResponse>> patch(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> body) {

        AgentTemplateEntity existing = templateService.findById(id);

        if (body.containsKey("basePrompt")) {
            existing.setBasePrompt((String) body.get("basePrompt"));
        }
        if (body.containsKey("allowedTools")) {
            Object toolsRaw = body.get("allowedTools");
            String toolsValue;
            if (toolsRaw instanceof String s) {
                // Accept both JSON array string and comma-separated
                if (s.trim().startsWith("[")) {
                    toolsValue = s.trim();
                } else {
                    // Fallback: convert comma-separated to JSON array
                    String[] parts = s.split(",");
                    StringBuilder json = new StringBuilder("[");
                    for (String part : parts) {
                        String name = part.trim();
                        if (!name.isEmpty()) {
                            if (json.length() > 1) json.append(",");
                            json.append("\"").append(name).append("\"");
                        }
                    }
                    json.append("]");
                    toolsValue = json.toString();
                }
            } else {
                toolsValue = "[]";
            }
            existing.setAllowedTools(toolsValue);
        }
        if (body.containsKey("maxTokensPerRun")) {
            existing.setMaxTokensPerRun(((Number) body.get("maxTokensPerRun")).intValue());
        }

        AgentTemplateEntity saved = templateService.patch(id, existing);
        return ResponseEntity.ok(ApiResponse.ok(AgentTemplateResponse.from(saved), "Agent template updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Agent template deleted"));
    }

    @GetMapping("/available-tools")
    public ResponseEntity<ApiResponse<List<AvailableToolResponse>>> availableTools() {
        List<AvailableToolResponse> tools = toolRegistry.getAllTools().stream()
                .map(tool -> new AvailableToolResponse(
                        tool.getName(),
                        tool.getDescription(),
                        tool.getPermission().name()
                ))
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(tools));
    }
}
