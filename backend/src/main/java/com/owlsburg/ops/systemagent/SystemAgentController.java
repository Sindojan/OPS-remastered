package com.owlsburg.ops.systemagent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentActivityStatus;
import com.owlsburg.ops.agentinfra.AgentInstanceStatus;
import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryEntity;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemAgentToolRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/agents")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemAgentController {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentController.class);

    private final SystemAgentInstanceService instanceService;
    private final SystemAgentTemplateRepository templateRepository;
    private final SystemAgentRunRepository runRepository;
    private final SystemAgentRunStepRepository runStepRepository;
    private final SystemAgentMemoryService memoryService;
    private final SystemAgentIncidentService incidentService;
    private final SystemAgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public SystemAgentController(SystemAgentInstanceService instanceService,
                                  SystemAgentTemplateRepository templateRepository,
                                  SystemAgentRunRepository runRepository,
                                  SystemAgentRunStepRepository runStepRepository,
                                  SystemAgentMemoryService memoryService,
                                  SystemAgentIncidentService incidentService,
                                  SystemAgentToolRegistry toolRegistry,
                                  ObjectMapper objectMapper) {
        this.instanceService = instanceService;
        this.templateRepository = templateRepository;
        this.runRepository = runRepository;
        this.runStepRepository = runStepRepository;
        this.memoryService = memoryService;
        this.incidentService = incidentService;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemAgentInstanceResponse>>> listAll() {
        List<SystemAgentInstanceEntity> instances = instanceService.findAll();
        List<SystemAgentInstanceResponse> responses = instances.stream()
                .map(inst -> {
                    SystemAgentTemplateEntity template = templateRepository.findById(inst.getTemplateId()).orElse(null);
                    String templateName = template != null ? template.getName() : "Unbekannt";
                    String role = template != null ? template.getRole() : null;
                    String model = resolveInstanceModel(inst);
                    List<String> tools = resolveInstanceTools(inst, template);
                    return new SystemAgentInstanceResponse(
                            inst.getId(), inst.getName(), templateName, role, model,
                            inst.getStatus().name(), inst.getActivityStatus().name(),
                            tools.size(), inst.getActivityStatusChangedAt());
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemAgentDetailResponse>> getById(@PathVariable UUID id) {
        SystemAgentInstanceEntity instance = instanceService.findById(id);
        SystemAgentTemplateEntity template = templateRepository.findById(instance.getTemplateId()).orElse(null);

        String model = resolveInstanceModel(instance);
        List<String> tools = resolveInstanceTools(instance, template);
        List<String> templateTools = parseTemplateTools(template);
        boolean hasToolOverride = instance.getAllowedToolsOverride() != null;

        int maxTokensPerRun = resolveConfigInt(instance, "maxTokensPerRun",
                template != null ? template.getMaxTokensPerRun() : 4096);
        int dailyTokenBudget = resolveConfigInt(instance, "dailyTokenBudget",
                template != null ? template.getDailyTokenBudget() : 100000);

        long openIncidents = incidentService.countOpenIncidents();

        return ResponseEntity.ok(ApiResponse.ok(new SystemAgentDetailResponse(
                instance.getId(), instance.getName(),
                template != null ? template.getName() : "Unbekannt",
                template != null ? template.getRole() : null,
                model, instance.getStatus().name(), instance.getActivityStatus().name(),
                instance.getCustomSystemPrompt(),
                tools, templateTools, hasToolOverride,
                maxTokensPerRun, dailyTokenBudget,
                template != null ? template.getMaxTokensPerRun() : 4096,
                template != null ? template.getDailyTokenBudget() : 100000,
                openIncidents, instance.getActivityStatusChangedAt())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateAgent(@PathVariable UUID id,
                                                          @Valid @RequestBody SystemAgentUpdateRequest request) {
        SystemAgentInstanceEntity inst = instanceService.findById(id);

        if (request.status() != null) {
            SystemAgentTemplateEntity template = templateRepository.findById(inst.getTemplateId()).orElse(null);
            if (template != null && "system_ceo".equals(template.getRole()) && "INACTIVE".equals(request.status())) {
                throw new IllegalArgumentException("System-CEO kann nicht deaktiviert werden");
            }
            inst.setStatus(AgentInstanceStatus.valueOf(request.status()));
        }

        if (request.customSystemPrompt() != null) {
            String prompt = request.customSystemPrompt().isBlank() ? null : request.customSystemPrompt();
            inst.setCustomSystemPrompt(prompt);
        }

        if (request.model() != null) {
            try {
                JsonNode config = objectMapper.readTree(inst.getConfig());
                var objNode = (com.fasterxml.jackson.databind.node.ObjectNode) config;
                objNode.put("model", request.model());
                inst.setConfig(objectMapper.writeValueAsString(objNode));
            } catch (Exception e) {
                var objNode = objectMapper.createObjectNode();
                objNode.put("model", request.model());
                inst.setConfig(objNode.toString());
            }
        }

        if (request.maxTokensPerRun() != null || request.dailyTokenBudget() != null) {
            try {
                JsonNode config = objectMapper.readTree(inst.getConfig());
                var objNode = (com.fasterxml.jackson.databind.node.ObjectNode) config;
                if (request.maxTokensPerRun() != null) {
                    objNode.put("maxTokensPerRun", request.maxTokensPerRun());
                }
                if (request.dailyTokenBudget() != null) {
                    objNode.put("dailyTokenBudget", request.dailyTokenBudget());
                }
                inst.setConfig(objectMapper.writeValueAsString(objNode));
            } catch (Exception e) {
                log.warn("Failed to update budget in instance config: {}", e.getMessage());
            }
        }

        if (request.allowedToolsOverride() != null) {
            if (request.allowedToolsOverride().isEmpty()) {
                inst.setAllowedToolsOverride(null);
            } else {
                List<String> validNames = toolRegistry.getAllToolNames();
                for (String toolName : request.allowedToolsOverride()) {
                    if (!validNames.contains(toolName)) {
                        throw new IllegalArgumentException("Unbekanntes System-Tool: " + toolName);
                    }
                }
                try {
                    inst.setAllowedToolsOverride(objectMapper.writeValueAsString(request.allowedToolsOverride()));
                } catch (Exception e) {
                    log.warn("Failed to serialize tool override: {}", e.getMessage());
                }
            }
        }

        instanceService.save(inst);
        return ResponseEntity.ok(ApiResponse.ok(null, "System-Agent aktualisiert"));
    }

    @GetMapping("/{id}/last-run")
    public ResponseEntity<ApiResponse<SystemLastRunResponse>> getLastRun(@PathVariable UUID id) {
        SystemAgentInstanceEntity instance = instanceService.findById(id);
        if (instance.getLastRunId() == null) {
            return ResponseEntity.ok(ApiResponse.ok(null, "Kein letzter Run vorhanden"));
        }
        SystemAgentRunEntity run = runRepository.findById(instance.getLastRunId()).orElse(null);
        if (run == null) {
            return ResponseEntity.ok(ApiResponse.ok(null, "Run nicht gefunden"));
        }
        List<SystemAgentRunStepEntity> steps = runStepRepository.findByRunIdOrderByStepNumber(run.getId());
        List<SystemLastRunStepResponse> stepResponses = steps.stream()
                .map(s -> new SystemLastRunStepResponse(
                        s.getId(), s.getType().name(), s.getToolName(),
                        s.getOutput(), s.getTokensUsed(), s.getDurationMs()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(new SystemLastRunResponse(
                run.getId(), run.getStatus().name(), run.getTokensUsed(),
                run.getErrorMessage(), run.getStartedAt(), run.getCompletedAt(), stepResponses)));
    }

    @GetMapping("/{id}/runs")
    public ResponseEntity<ApiResponse<Page<SystemAgentRunSummary>>> getRuns(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SystemAgentRunEntity> runs = runRepository.findByInstanceId(
                id, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt")));
        Page<SystemAgentRunSummary> summaries = runs.map(r -> new SystemAgentRunSummary(
                r.getId(), r.getStatus().name(), r.getTriggerType().name(),
                r.getTokensUsed(), r.getCostUsd(), r.getStartedAt(), r.getCompletedAt(),
                r.getErrorMessage()));
        return ResponseEntity.ok(ApiResponse.ok(summaries));
    }

    @GetMapping("/{id}/memories")
    public ResponseEntity<ApiResponse<List<SystemAgentMemorySummary>>> getMemories(
            @PathVariable UUID id,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "50") int limit) {
        List<SystemAgentMemoryEntity> memories = memoryService.recallMemories(id, category, limit);
        List<SystemAgentMemorySummary> summaries = memories.stream()
                .map(m -> new SystemAgentMemorySummary(
                        m.getId(), m.getType(), m.getCategory(), m.getKey(),
                        m.getValue(), m.getImportance(), m.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(summaries));
    }

    @GetMapping("/{id}/incidents")
    public ResponseEntity<ApiResponse<List<SystemAgentIncidentSummary>>> getIncidents(@PathVariable UUID id) {
        List<SystemAgentIncidentEntity> incidents = incidentService.findByInstance(id);
        List<SystemAgentIncidentSummary> summaries = incidents.stream()
                .map(i -> new SystemAgentIncidentSummary(
                        i.getId(), i.getType(), i.getDescription(),
                        i.getCreatedAt(), i.getResolvedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(summaries));
    }

    @GetMapping("/available-tools")
    public ResponseEntity<ApiResponse<List<SystemToolInfo>>> getAvailableTools() {
        List<SystemToolInfo> tools = toolRegistry.getAllTools().stream()
                .map(t -> new SystemToolInfo(t.getName(), t.getDescription()))
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(tools));
    }

    // ─── Helper methods ─────────────────────────────────────

    private String resolveInstanceModel(SystemAgentInstanceEntity instance) {
        try {
            JsonNode config = objectMapper.readTree(instance.getConfig());
            if (config.has("model")) {
                return config.get("model").asText();
            }
        } catch (Exception ignored) {}
        return "claude-sonnet-4-6";
    }

    private List<String> resolveInstanceTools(SystemAgentInstanceEntity inst, SystemAgentTemplateEntity template) {
        if (inst.getAllowedToolsOverride() != null) {
            try {
                return objectMapper.readValue(inst.getAllowedToolsOverride(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse allowedToolsOverride: {}", e.getMessage());
            }
        }
        return parseTemplateTools(template);
    }

    private List<String> parseTemplateTools(SystemAgentTemplateEntity template) {
        if (template == null) return List.of();
        try {
            return objectMapper.readValue(template.getAllowedTools(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private int resolveConfigInt(SystemAgentInstanceEntity inst, String key, int defaultValue) {
        if (inst.getConfig() != null && !"{}".equals(inst.getConfig())) {
            try {
                JsonNode config = objectMapper.readTree(inst.getConfig());
                if (config.has(key)) return config.get(key).asInt();
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    // ─── DTOs ───────────────────────────────────────────────

    public record SystemAgentInstanceResponse(
            UUID id, String name, String templateName, String role, String model,
            String status, String activityStatus, int toolCount, Instant lastActivity) {}

    public record SystemAgentDetailResponse(
            UUID id, String name, String templateName, String role, String model,
            String status, String activityStatus, String customSystemPrompt,
            List<String> allowedTools, List<String> templateTools, boolean hasToolOverride,
            int maxTokensPerRun, int dailyTokenBudget,
            int templateMaxTokensPerRun, int templateDailyTokenBudget,
            long openIncidents, Instant lastActivity) {}

    public record SystemAgentUpdateRequest(String customSystemPrompt, String model, String status,
                                            Integer maxTokensPerRun, Integer dailyTokenBudget,
                                            List<String> allowedToolsOverride) {}

    public record SystemAgentRunSummary(UUID id, String status, String triggerType,
                                         int tokensUsed, java.math.BigDecimal costUsd,
                                         Instant startedAt, Instant completedAt,
                                         String errorMessage) {}

    public record SystemAgentMemorySummary(UUID id, String type, String category, String key,
                                            String value, int importance, Instant createdAt) {}

    public record SystemAgentIncidentSummary(UUID id, String type, String description,
                                              Instant createdAt, Instant resolvedAt) {}

    public record SystemToolInfo(String name, String description) {}

    public record SystemLastRunResponse(UUID id, String status, int tokensUsed,
                                         String errorMessage, Instant startedAt, Instant completedAt,
                                         List<SystemLastRunStepResponse> steps) {}

    public record SystemLastRunStepResponse(UUID id, String type, String toolName,
                                             String output, int tokensUsed, Integer durationMs) {}
}
