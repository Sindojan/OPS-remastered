package com.sindoflow.ops.people;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.people.dto.AbsenceResponse;
import com.sindoflow.ops.people.dto.CreateAbsenceRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/absences")
public class AbsenceController {

    private final AbsenceService absenceService;

    public AbsenceController(AbsenceService absenceService) {
        this.absenceService = absenceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AbsenceResponse>> request(@Valid @RequestBody CreateAbsenceRequest request) {
        AbsenceEntity entity = absenceService.request(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(AbsenceResponse.from(entity)));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AbsenceResponse>> approve(@PathVariable UUID id) {
        AbsenceEntity entity = absenceService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(AbsenceResponse.from(entity)));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<AbsenceResponse>> reject(@PathVariable UUID id) {
        AbsenceEntity entity = absenceService.reject(id);
        return ResponseEntity.ok(ApiResponse.ok(AbsenceResponse.from(entity)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AbsenceResponse>>> findAbsences(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<AbsenceEntity> results;
        if (employeeId != null) {
            results = absenceService.findByEmployee(employeeId);
        } else if (from != null && to != null) {
            results = absenceService.findByDateRange(from, to);
        } else {
            results = absenceService.findByEmployee(null);
        }
        List<AbsenceResponse> list = results.stream()
                .map(AbsenceResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
