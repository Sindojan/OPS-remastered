package com.sindoflow.ops.bom;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.bom.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/calculations")
public class CalculationController {

    private final CalculationService calculationService;
    private final JobCalculationService jobCalculationService;

    public CalculationController(CalculationService calculationService,
                                 JobCalculationService jobCalculationService) {
        this.calculationService = calculationService;
        this.jobCalculationService = jobCalculationService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<CalculationResponse>> calculate(
            @Valid @RequestBody CalculateRequest request) {
        CalculationEntity entity = calculationService.calculate(
                request.partId(), request.bomVersionId(), request.processPlanId(),
                request.quantity(), request.calculatedBy()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(CalculationResponse.from(entity)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CalculationResponse>> getById(@PathVariable UUID id) {
        CalculationEntity entity = calculationService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(CalculationResponse.from(entity)));
    }

    @PostMapping("/job-calculations")
    public ResponseEntity<ApiResponse<JobCalculationResponse>> createJobCalculation(
            @Valid @RequestBody CreateJobCalculationRequest request) {
        JobCalculationEntity entity = jobCalculationService.createFromCalculation(
                request.jobId(), request.calculationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(JobCalculationResponse.from(entity)));
    }

    @PatchMapping("/job-calculations/{id}/finalize")
    public ResponseEntity<ApiResponse<JobCalculationResponse>> finalizeJobCalculation(
            @PathVariable UUID id, @Valid @RequestBody FinalizeJobCalculationRequest request) {
        JobCalculationEntity entity = jobCalculationService.finalize(
                id, request.actualMaterialCost(), request.actualLaborCost());
        return ResponseEntity.ok(ApiResponse.ok(JobCalculationResponse.from(entity)));
    }
}
