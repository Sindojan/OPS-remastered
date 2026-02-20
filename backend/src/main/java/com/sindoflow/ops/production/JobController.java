package com.sindoflow.ops.production;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.production.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> create(@Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Job created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getAll(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) UUID customerId,
            Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.ok(jobService.findByStatus(status, pageable)));
        }
        if (customerId != null) {
            return ResponseEntity.ok(ApiResponse.ok(jobService.findByCustomer(customerId, pageable)));
        }
        return ResponseEntity.ok(ApiResponse.ok(jobService.getAll(pageable)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> update(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateJobRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.update(id, request), "Job updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        jobService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Job deleted"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<JobResponse>> changeStatus(@PathVariable UUID id,
                                                                  @Valid @RequestBody ChangeJobStatusRequest request) {
        JobResponse response = jobService.changeStatus(id, request.newStatus(), request.changedBy(), request.reason());
        return ResponseEntity.ok(ApiResponse.ok(response, "Job status updated"));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<JobResponse>> assign(@PathVariable UUID id,
                                                            @Valid @RequestBody AssignJobRequest request) {
        JobResponse response = jobService.assignToStation(id, request.stationId(), request.shiftId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Job assigned to station"));
    }
}
