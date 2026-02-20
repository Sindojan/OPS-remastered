package com.sindoflow.ops.production;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.production.dto.CreateShiftRequest;
import com.sindoflow.ops.production.dto.ShiftResponse;
import com.sindoflow.ops.production.dto.UpdateShiftRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShiftResponse>> create(@Valid @RequestBody CreateShiftRequest request) {
        ShiftResponse response = shiftService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Shift created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShiftResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getAll()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShiftResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdateShiftRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.update(id, request), "Shift updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        shiftService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Shift deleted"));
    }
}
