package com.sindoflow.ops.bom;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.bom.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/process-plans")
public class ProcessPlanController {

    private final ProcessPlanService planService;

    public ProcessPlanController(ProcessPlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProcessPlanResponse>> createPlan(
            @Valid @RequestBody CreateProcessPlanRequest request) {
        ProcessPlanEntity entity = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ProcessPlanResponse.from(entity)));
    }

    @PostMapping("/{id}/steps")
    public ResponseEntity<ApiResponse<ProcessStepResponse>> addStep(
            @PathVariable UUID id, @Valid @RequestBody ProcessStepRequest request) {
        ProcessStepEntity entity = planService.addStep(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ProcessStepResponse.from(entity)));
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<ApiResponse<List<ProcessStepResponse>>> getSteps(@PathVariable UUID id) {
        List<ProcessStepResponse> steps = planService.getSteps(id).stream()
                .map(ProcessStepResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(steps));
    }

    @PutMapping("/steps/{stepId}")
    public ResponseEntity<ApiResponse<ProcessStepResponse>> updateStep(
            @PathVariable UUID stepId, @Valid @RequestBody ProcessStepRequest request) {
        ProcessStepEntity entity = planService.updateStep(stepId, request);
        return ResponseEntity.ok(ApiResponse.ok(ProcessStepResponse.from(entity)));
    }

    @DeleteMapping("/steps/{stepId}")
    public ResponseEntity<ApiResponse<Void>> removeStep(@PathVariable UUID stepId) {
        planService.removeStep(stepId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Process step removed"));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProcessPlanResponse>> activatePlan(@PathVariable UUID id) {
        ProcessPlanEntity entity = planService.activatePlan(id);
        return ResponseEntity.ok(ApiResponse.ok(ProcessPlanResponse.from(entity)));
    }
}
