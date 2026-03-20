package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.*;
import com.owlsburg.ops.agentinfra.execution.AgentRunOrchestrator;
import com.owlsburg.ops.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent-runs")
public class AgentRunController {

    private final AgentRunService runService;
    private final AgentRunOrchestrator orchestrator;

    public AgentRunController(AgentRunService runService,
                               AgentRunOrchestrator orchestrator) {
        this.runService = runService;
        this.orchestrator = orchestrator;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AgentRunResponse>> startRun(
            @Valid @RequestBody StartAgentRunRequest request) {

        AgentRunEntity run = runService.startRun(
                request.instanceId(),
                request.triggerType(),
                request.triggerSource(),
                request.inputContext()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(AgentRunResponse.from(run), "Agent run started"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentRunResponse>> getById(@PathVariable UUID id) {
        AgentRunEntity run = runService.findById(id);
        List<AgentRunStepResponse> steps = runService.findStepsByRunId(id).stream()
                .map(AgentRunStepResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(AgentRunResponse.from(run, steps)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgentRunResponse>>> list(
            @RequestParam(value = "instanceId", required = false) UUID instanceId,
            @RequestParam(value = "status", required = false) AgentRunStatus status) {

        List<AgentRunEntity> runs;
        if (instanceId != null) {
            runs = runService.findByInstanceId(instanceId);
        } else if (status != null) {
            runs = runService.findByStatus(status);
        } else {
            runs = runService.findAll();
        }

        List<AgentRunResponse> response = runs.stream()
                .map(AgentRunResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/steps")
    public ResponseEntity<ApiResponse<AgentRunStepResponse>> addStep(
            @PathVariable UUID id,
            @Valid @RequestBody AddAgentRunStepRequest request) {

        AgentRunStepEntity step = runService.addStep(
                id,
                request.type(),
                request.toolName(),
                request.input(),
                request.output(),
                request.tokensUsed(),
                request.durationMs()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(AgentRunStepResponse.from(step), "Step added"));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AgentRunResponse>> completeRun(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteAgentRunRequest request) {

        AgentRunEntity run = runService.completeRun(id, request.output(), request.totalTokensUsed(), request.costUsd());
        return ResponseEntity.ok(ApiResponse.ok(AgentRunResponse.from(run), "Agent run completed"));
    }

    @PatchMapping("/{id}/fail")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AgentRunResponse>> failRun(
            @PathVariable UUID id,
            @Valid @RequestBody FailAgentRunRequest request) {

        AgentRunEntity run = runService.failRun(id, request.errorMessage());
        return ResponseEntity.ok(ApiResponse.ok(AgentRunResponse.from(run), "Agent run failed"));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AgentRunResponse>> cancelRun(@PathVariable UUID id) {
        AgentRunEntity run = runService.cancelRun(id);
        return ResponseEntity.ok(ApiResponse.ok(AgentRunResponse.from(run), "Agent run cancelled"));
    }

    @GetMapping("/budget/{instanceId}")
    public ResponseEntity<ApiResponse<BudgetCheckResponse>> checkBudget(@PathVariable UUID instanceId) {
        AgentRunService.BudgetCheckResult result = runService.checkBudget(instanceId);
        return ResponseEntity.ok(ApiResponse.ok(BudgetCheckResponse.from(result)));
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> triggerRun(
            @Valid @RequestBody StartAgentRunRequest request) {
        orchestrator.triggerRun(
                request.instanceId(),
                request.triggerType(),
                request.triggerSource(),
                request.inputContext()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Agent run triggered", "Run wird asynchron ausgeführt"));
    }
}
