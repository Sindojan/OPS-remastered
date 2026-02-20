package com.sindoflow.ops.machines;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.machines.dto.MachineIncidentResponse;
import com.sindoflow.ops.machines.dto.ReportIncidentRequest;
import com.sindoflow.ops.machines.dto.ResolveIncidentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/machines/{machineId}/incidents")
public class MachineIncidentController {

    private final MachineIncidentService incidentService;

    public MachineIncidentController(MachineIncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MachineIncidentResponse>> report(
            @PathVariable UUID machineId,
            @Valid @RequestBody ReportIncidentRequest request) {
        MachineIncidentResponse response = incidentService.report(machineId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Incident reported"));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<MachineIncidentResponse>> resolve(
            @PathVariable UUID machineId,
            @PathVariable UUID id,
            @Valid @RequestBody ResolveIncidentRequest request) {
        MachineIncidentResponse response = incidentService.resolve(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Incident resolved"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MachineIncidentResponse>>> listByMachine(
            @PathVariable UUID machineId) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.findByMachine(machineId)));
    }
}
