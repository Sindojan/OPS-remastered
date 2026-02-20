package com.sindoflow.ops.production;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.production.dto.CreateStationRequest;
import com.sindoflow.ops.production.dto.StationResponse;
import com.sindoflow.ops.production.dto.UpdateStationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StationResponse>> create(@Valid @RequestBody CreateStationRequest request) {
        StationResponse response = stationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Station created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(stationService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(stationService.getAll()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StationResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateStationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(stationService.update(id, request), "Station updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        stationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Station deleted"));
    }

    @PostMapping("/{id}/shifts/{shiftId}")
    public ResponseEntity<ApiResponse<StationResponse>> addShift(@PathVariable UUID id,
                                                                  @PathVariable UUID shiftId) {
        return ResponseEntity.ok(ApiResponse.ok(stationService.addShift(id, shiftId), "Shift added to station"));
    }

    @DeleteMapping("/{id}/shifts/{shiftId}")
    public ResponseEntity<ApiResponse<StationResponse>> removeShift(@PathVariable UUID id,
                                                                     @PathVariable UUID shiftId) {
        return ResponseEntity.ok(ApiResponse.ok(stationService.removeShift(id, shiftId), "Shift removed from station"));
    }
}
