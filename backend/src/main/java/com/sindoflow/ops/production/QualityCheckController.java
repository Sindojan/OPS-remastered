package com.sindoflow.ops.production;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.production.dto.CreateQualityCheckRequest;
import com.sindoflow.ops.production.dto.CreateQualityDefectRequest;
import com.sindoflow.ops.production.dto.QualityCheckResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quality-checks")
public class QualityCheckController {

    private final QualityCheckService qualityCheckService;

    public QualityCheckController(QualityCheckService qualityCheckService) {
        this.qualityCheckService = qualityCheckService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QualityCheckResponse>> create(
            @Valid @RequestBody CreateQualityCheckRequest request) {
        QualityCheckResponse response = qualityCheckService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Quality check created"));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<List<QualityCheckResponse>>> findByJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(qualityCheckService.findByJob(jobId)));
    }

    @PostMapping("/{id}/defects")
    public ResponseEntity<ApiResponse<QualityCheckResponse>> addDefect(
            @PathVariable UUID id,
            @Valid @RequestBody CreateQualityDefectRequest request) {
        QualityCheckResponse response = qualityCheckService.addDefect(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Defect added"));
    }
}
