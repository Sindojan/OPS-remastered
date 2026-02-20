package com.sindoflow.ops.machines;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.machines.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/intervals")
    public ResponseEntity<ApiResponse<List<MaintenanceIntervalResponse>>> getIntervals(
            @RequestParam UUID machineId) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getIntervalsByMachine(machineId)));
    }

    @PostMapping("/intervals")
    public ResponseEntity<ApiResponse<MaintenanceIntervalResponse>> createInterval(
            @Valid @RequestBody CreateMaintenanceIntervalRequest request) {
        MaintenanceIntervalResponse response = maintenanceService.createInterval(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Maintenance interval created"));
    }

    @PostMapping("/records")
    public ResponseEntity<ApiResponse<MaintenanceRecordResponse>> recordMaintenance(
            @Valid @RequestBody PerformMaintenanceRequest request) {
        MaintenanceRecordResponse response = maintenanceService.performMaintenance(
                request.machineId(), request.intervalId(),
                request.performedBy(), request.durationMinutes(), request.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Maintenance recorded"));
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<MaintenanceIntervalResponse>>> getOverdue() {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getOverdue()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<MaintenanceIntervalResponse>>> getUpcoming(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getUpcoming(days)));
    }
}
