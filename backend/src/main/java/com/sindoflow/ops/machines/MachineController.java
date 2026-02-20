package com.sindoflow.ops.machines;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.machines.dto.ChangeMachineStatusRequest;
import com.sindoflow.ops.machines.dto.CreateMachineRequest;
import com.sindoflow.ops.machines.dto.MachineResponse;
import com.sindoflow.ops.machines.dto.UpdateMachineRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MachineResponse>> create(@Valid @RequestBody CreateMachineRequest request) {
        MachineResponse response = machineService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Machine created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(machineService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MachineResponse>>> getAll(
            @RequestParam(required = false) UUID stationId) {
        if (stationId != null) {
            return ResponseEntity.ok(ApiResponse.ok(machineService.getByStation(stationId)));
        }
        return ResponseEntity.ok(ApiResponse.ok(machineService.getAll()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateMachineRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(machineService.update(id, request), "Machine updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        machineService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Machine deleted"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MachineResponse>> changeStatus(@PathVariable UUID id,
                                                                      @Valid @RequestBody ChangeMachineStatusRequest request) {
        MachineResponse response = machineService.changeStatus(id, request.newStatus());
        return ResponseEntity.ok(ApiResponse.ok(response, "Machine status updated"));
    }
}
