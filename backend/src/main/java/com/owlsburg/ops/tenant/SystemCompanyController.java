package com.owlsburg.ops.tenant;

import com.owlsburg.ops.agentinfra.*;
import com.owlsburg.ops.agentinfra.dto.BudgetOverviewResponse;
import com.owlsburg.ops.agentinfra.llm.LlmConfigService;
import com.owlsburg.ops.agentinfra.llm.TenantLlmConfigEntity;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.auth.UserEntity;
import com.owlsburg.ops.auth.UserService;
import com.owlsburg.ops.auth.dto.UserResponse;
import com.owlsburg.ops.common.*;
import com.owlsburg.ops.common.dto.ModuleResponse;
import com.owlsburg.ops.tenant.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/companies")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemCompanyController {

    private static final Logger log = LoggerFactory.getLogger(SystemCompanyController.class);

    private final SystemCompanyService companyService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ModuleService moduleService;
    private final BudgetService budgetService;
    private final AgentInstanceService agentInstanceService;
    private final AgentTemplateRepository templateRepository;
    private final LlmConfigService llmConfigService;
    private final AgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public SystemCompanyController(SystemCompanyService companyService, UserService userService,
                                   PasswordEncoder passwordEncoder, ModuleService moduleService,
                                   BudgetService budgetService, AgentInstanceService agentInstanceService,
                                   AgentTemplateRepository templateRepository, LlmConfigService llmConfigService,
                                   AgentToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.companyService = companyService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.moduleService = moduleService;
        this.budgetService = budgetService;
        this.agentInstanceService = agentInstanceService;
        this.templateRepository = templateRepository;
        this.llmConfigService = llmConfigService;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
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

    // ─── Module Management ─────────────────────────────────

    @GetMapping("/{id}/modules")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getModules(@PathVariable UUID id) {
        return withTenantContext(id, () -> {
            List<ModuleEntity> allModules = moduleService.getAllModules();
            Set<String> enabled = moduleService.getEnabledModules(id);

            List<ModuleResponse> responses = allModules.stream()
                    .map(m -> new ModuleResponse(
                            m.getId(), m.getLabel(), m.getDescription(),
                            m.isCore(), m.getDisplayOrder(),
                            m.isCore() || enabled.contains(m.getId())
                    ))
                    .toList();

            return ResponseEntity.ok(ApiResponse.ok(responses));
        });
    }

    @PutMapping("/{id}/modules/{moduleId}/toggle")
    public ResponseEntity<ApiResponse<ModuleResponse>> toggleModule(
            @PathVariable UUID id,
            @PathVariable String moduleId,
            @RequestBody ModuleToggleRequest request) {
        return withTenantContext(id, () -> {
            moduleService.toggleModule(id, moduleId, request.enabled());

            ModuleEntity module = moduleService.getAllModules().stream()
                    .filter(m -> m.getId().equals(moduleId))
                    .findFirst()
                    .orElseThrow();

            ModuleResponse response = new ModuleResponse(
                    module.getId(), module.getLabel(), module.getDescription(),
                    module.isCore(), module.getDisplayOrder(), request.enabled()
            );

            return ResponseEntity.ok(ApiResponse.ok(response));
        });
    }

    public record ModuleToggleRequest(boolean enabled) {}

    // ─── Budget ────────────────────────────────────────────

    @GetMapping("/{id}/budget")
    public ResponseEntity<ApiResponse<BudgetOverviewResponse>> getBudget(@PathVariable UUID id) {
        companyService.findById(id); // ensure exists
        return withTenantContext(id, () ->
                ResponseEntity.ok(ApiResponse.ok(budgetService.getOverview())));
    }

    // ─── Agent Management ──────────────────────────────────

    @GetMapping("/{id}/agents")
    public ResponseEntity<ApiResponse<List<AgentInstanceDetailResponse>>> getAgents(@PathVariable UUID id) {
        companyService.findById(id); // ensure exists
        return withTenantContext(id, () -> {
            List<AgentInstanceEntity> instances = agentInstanceService.findAll();
            List<AgentInstanceDetailResponse> responses = instances.stream()
                    .map(inst -> {
                        AgentTemplateEntity template = templateRepository.findById(inst.getTemplateId())
                                .orElse(null);
                        String templateName = template != null ? template.getName() : "Unbekannt";

                        // Resolve effective tools
                        List<String> allowedTools = resolveInstanceTools(inst, template);
                        List<String> templateTools = parseTemplateTools(template);
                        boolean hasToolOverride = inst.getAllowedToolsOverride() != null;

                        // Resolve budget from instance config or template
                        int maxTokensPerRun = resolveConfigInt(inst, "maxTokensPerRun",
                                template != null ? template.getMaxTokensPerRun() : 4096);
                        int dailyTokenBudget = resolveConfigInt(inst, "dailyTokenBudget",
                                template != null ? template.getDailyTokenBudget() : 100000);

                        String model = resolveInstanceModel(inst);
                        return new AgentInstanceDetailResponse(
                                inst.getId(), inst.getName(), templateName, model,
                                inst.getStatus().name(), inst.getActivityStatus().name(),
                                allowedTools.size(), inst.getCustomSystemPrompt() != null,
                                inst.getCustomSystemPrompt(),
                                allowedTools, templateTools, hasToolOverride,
                                maxTokensPerRun, dailyTokenBudget,
                                template != null ? template.getMaxTokensPerRun() : 4096,
                                template != null ? template.getDailyTokenBudget() : 100000);
                    })
                    .toList();
            return ResponseEntity.ok(ApiResponse.ok(responses));
        });
    }

    @GetMapping("/{id}/agent-summary")
    public ResponseEntity<ApiResponse<AgentSystemSummaryResponse>> getAgentSummary(@PathVariable UUID id) {
        companyService.findById(id); // ensure exists
        return withTenantContext(id, () -> {
            List<AgentInstanceEntity> instances = agentInstanceService.findAll();
            int total = instances.size();
            int active = (int) instances.stream()
                    .filter(i -> i.getStatus() == AgentInstanceStatus.ACTIVE).count();
            int error = (int) instances.stream()
                    .filter(i -> i.getActivityStatus() == AgentActivityStatus.ERROR).count();
            List<String> modelsInUse = instances.stream()
                    .map(this::resolveInstanceModel)
                    .distinct()
                    .toList();
            long totalDailyBudget = instances.stream()
                    .filter(i -> i.getStatus() == AgentInstanceStatus.ACTIVE)
                    .mapToLong(i -> {
                        AgentTemplateEntity t = templateRepository.findById(i.getTemplateId()).orElse(null);
                        return resolveConfigInt(i, "dailyTokenBudget",
                                t != null ? t.getDailyTokenBudget() : 100000);
                    })
                    .sum();
            Instant lastActivity = instances.stream()
                    .map(AgentInstanceEntity::getActivityStatusChangedAt)
                    .filter(java.util.Objects::nonNull)
                    .max(Instant::compareTo)
                    .orElse(null);

            return ResponseEntity.ok(ApiResponse.ok(new AgentSystemSummaryResponse(
                    total, active, error, modelsInUse, totalDailyBudget, lastActivity)));
        });
    }

    @GetMapping("/{id}/available-tools")
    public ResponseEntity<ApiResponse<List<ToolInfoResponse>>> getAvailableTools(@PathVariable UUID id) {
        companyService.findById(id); // ensure exists
        List<ToolInfoResponse> tools = toolRegistry.getAllTools().stream()
                .map(t -> new ToolInfoResponse(t.getName(), t.getDescription(), t.getModuleId()))
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(tools));
    }

    private static final Set<String> VALID_MODELS = Set.of(
            "claude-opus-4-20250514", "claude-sonnet-4-20250514", "claude-haiku-4-5-20251001");

    @PatchMapping("/{id}/agents/{instanceId}")
    public ResponseEntity<ApiResponse<Void>> updateAgent(@PathVariable UUID id, @PathVariable UUID instanceId,
                                                          @RequestBody AgentUpdateRequest request) {
        companyService.findById(id); // ensure exists
        return withTenantContext(id, () -> {
            AgentInstanceEntity inst = agentInstanceService.findById(instanceId);
            // Defense-in-depth: verify instance belongs to target tenant
            if (!inst.getTenantId().equals(id)) {
                throw new org.springframework.security.access.AccessDeniedException("Zugriff verweigert");
            }

            // Status toggle (CEO cannot be deactivated)
            if (request.status() != null) {
                AgentTemplateEntity template = templateRepository.findById(inst.getTemplateId()).orElse(null);
                if (template != null && "ceo".equals(template.getRole()) && "INACTIVE".equals(request.status())) {
                    throw new IllegalArgumentException("CEO-Agent kann nicht deaktiviert werden");
                }
                inst.setStatus(AgentInstanceStatus.valueOf(request.status()));
            }

            if (request.customSystemPrompt() != null) {
                String prompt = request.customSystemPrompt().isBlank() ? null : request.customSystemPrompt();
                inst.setCustomSystemPrompt(prompt);
            }
            if (request.model() != null) {
                if (!VALID_MODELS.contains(request.model())) {
                    throw new IllegalArgumentException("Ungültiges Model: " + request.model());
                }
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

            // Budget overrides in instance config
            if (request.maxTokensPerRun() != null || request.dailyTokenBudget() != null) {
                try {
                    JsonNode config = objectMapper.readTree(inst.getConfig());
                    var objNode = (com.fasterxml.jackson.databind.node.ObjectNode) config;
                    if (request.maxTokensPerRun() != null) {
                        if (request.maxTokensPerRun() < 256 || request.maxTokensPerRun() > 8192) {
                            throw new IllegalArgumentException("maxTokensPerRun muss zwischen 256 und 8192 liegen");
                        }
                        objNode.put("maxTokensPerRun", request.maxTokensPerRun());
                    }
                    if (request.dailyTokenBudget() != null) {
                        objNode.put("dailyTokenBudget", request.dailyTokenBudget());
                    }
                    inst.setConfig(objectMapper.writeValueAsString(objNode));
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("Failed to update budget in instance config: {}", e.getMessage());
                }
            }

            // Tool override
            if (request.allowedToolsOverride() != null) {
                if (request.allowedToolsOverride().isEmpty()) {
                    // Empty list = reset to template defaults
                    inst.setAllowedToolsOverride(null);
                } else {
                    // Validate tool names
                    List<String> validNames = toolRegistry.getAllToolNames();
                    for (String toolName : request.allowedToolsOverride()) {
                        if (!validNames.contains(toolName)) {
                            throw new IllegalArgumentException("Unbekanntes Tool: " + toolName);
                        }
                    }
                    try {
                        inst.setAllowedToolsOverride(objectMapper.writeValueAsString(request.allowedToolsOverride()));
                    } catch (Exception e) {
                        log.warn("Failed to serialize tool override: {}", e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Agent aktualisiert"));
        });
    }

    public record AgentInstanceDetailResponse(
            UUID instanceId, String name, String templateName, String model,
            String status, String activityStatus, int toolCount, boolean hasCustomPrompt,
            String customSystemPrompt,
            List<String> allowedTools, List<String> templateTools, boolean hasToolOverride,
            int maxTokensPerRun, int dailyTokenBudget,
            int templateMaxTokensPerRun, int templateDailyTokenBudget) {}

    public record AgentUpdateRequest(String customSystemPrompt, String model, String status,
                                     Integer maxTokensPerRun, Integer dailyTokenBudget,
                                     List<String> allowedToolsOverride) {}

    public record AgentSystemSummaryResponse(int totalAgents, int activeAgents, int errorAgents,
                                              List<String> modelsInUse, long totalDailyBudget,
                                              Instant lastActivity) {}

    public record ToolInfoResponse(String name, String description, String moduleId) {}

    // ─── LLM Config ────────────────────────────────────────

    @GetMapping("/{id}/llm-config")
    public ResponseEntity<ApiResponse<LlmConfigResponse>> getLlmConfig(@PathVariable UUID id) {
        companyService.findById(id); // ensure exists
        return withTenantContext(id, () -> {
            Optional<TenantLlmConfigEntity> config = llmConfigService.getConfig();
            LlmConfigResponse response = config.map(c -> new LlmConfigResponse(
                    c.getProvider(), c.getDefaultModel(),
                    c.getApiKeyEnc() != null && !c.getApiKeyEnc().isBlank(),
                    llmConfigService.getTemperature(c),
                    llmConfigService.getMaxTokensDefault(c)
            )).orElse(new LlmConfigResponse("anthropic", null, false, null, null));
            return ResponseEntity.ok(ApiResponse.ok(response));
        });
    }

    private static final Set<String> VALID_PROVIDERS = Set.of("anthropic");

    @PutMapping("/{id}/llm-config")
    public ResponseEntity<ApiResponse<LlmConfigResponse>> saveLlmConfig(@PathVariable UUID id,
                                                                          @Valid @RequestBody LlmConfigSaveRequest request) {
        companyService.findById(id); // ensure exists
        String provider = request.provider() != null ? request.provider() : "anthropic";
        if (!VALID_PROVIDERS.contains(provider)) {
            throw new IllegalArgumentException("Ungültiger Provider: " + provider);
        }
        if (request.temperature() != null && (request.temperature() < 0.0 || request.temperature() > 1.0)) {
            throw new IllegalArgumentException("Temperature muss zwischen 0.0 und 1.0 liegen");
        }
        if (request.maxTokensDefault() != null && (request.maxTokensDefault() < 256 || request.maxTokensDefault() > 8192)) {
            throw new IllegalArgumentException("maxTokensDefault muss zwischen 256 und 8192 liegen");
        }
        return withTenantContext(id, () -> {
            TenantLlmConfigEntity saved = llmConfigService.saveConfigWithSettings(
                    provider, request.apiKey(), request.defaultModel(),
                    request.temperature(), request.maxTokensDefault());
            LlmConfigResponse response = new LlmConfigResponse(
                    saved.getProvider(), saved.getDefaultModel(), true,
                    llmConfigService.getTemperature(saved),
                    llmConfigService.getMaxTokensDefault(saved));
            return ResponseEntity.ok(ApiResponse.ok(response));
        });
    }

    @GetMapping("/{id}/llm-config/models")
    public ResponseEntity<ApiResponse<List<String>>> listModels(@PathVariable UUID id) {
        companyService.findById(id);
        return withTenantContext(id, () -> {
            List<String> models = llmConfigService.listModels();
            return ResponseEntity.ok(ApiResponse.ok(models));
        });
    }

    @PostMapping("/{id}/llm-config/models")
    public ResponseEntity<ApiResponse<List<String>>> listModelsWithKey(@PathVariable UUID id,
                                                                        @RequestBody LlmConfigSaveRequest request) {
        companyService.findById(id);
        try {
            List<String> models = llmConfigService.listModelsWithKey(
                    request.provider(), request.apiKey());
            return ResponseEntity.ok(ApiResponse.ok(models));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Verbindungsfehler: " + e.getMessage()));
        }
    }

    public record LlmConfigResponse(String provider, String defaultModel, boolean hasApiKey,
                                     Double temperature, Integer maxTokensDefault) {}
    public record LlmConfigSaveRequest(
            String provider,
            @jakarta.validation.constraints.Size(max = 200) String apiKey,
            String defaultModel,
            Double temperature,
            Integer maxTokensDefault) {}

    private String resolveInstanceModel(AgentInstanceEntity instance) {
        try {
            JsonNode config = objectMapper.readTree(instance.getConfig());
            if (config.has("model")) {
                return config.get("model").asText();
            }
        } catch (Exception ignored) {}
        return "claude-sonnet-4-20250514";
    }

    private List<String> resolveInstanceTools(AgentInstanceEntity inst, AgentTemplateEntity template) {
        if (inst.getAllowedToolsOverride() != null) {
            try {
                return objectMapper.readValue(inst.getAllowedToolsOverride(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse allowedToolsOverride: {}", e.getMessage());
            }
        }
        return parseTemplateTools(template);
    }

    private List<String> parseTemplateTools(AgentTemplateEntity template) {
        if (template == null) return List.of();
        try {
            return objectMapper.readValue(template.getAllowedTools(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private int resolveConfigInt(AgentInstanceEntity inst, String key, int defaultValue) {
        if (inst.getConfig() != null && !"{}".equals(inst.getConfig())) {
            try {
                JsonNode config = objectMapper.readTree(inst.getConfig());
                if (config.has(key)) return config.get(key).asInt();
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    /**
     * Executes the given operation with the tenant context temporarily set.
     * Required because SYSTEM_ADMIN has no tenant context, but RLS on tenant_modules
     * requires it.
     */
    private <T> T withTenantContext(UUID tenantId, java.util.function.Supplier<T> operation) {
        try {
            TenantContext.setCurrentTenant(tenantId.toString());
            return operation.get();
        } finally {
            TenantContext.clear();
        }
    }
}
