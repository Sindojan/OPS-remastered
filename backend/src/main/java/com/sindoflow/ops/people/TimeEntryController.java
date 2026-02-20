package com.sindoflow.ops.people;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.people.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-entries")
public class TimeEntryController {

    private final TimeTrackingService timeTrackingService;

    public TimeEntryController(TimeTrackingService timeTrackingService) {
        this.timeTrackingService = timeTrackingService;
    }

    @PostMapping("/clock-in")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> clockIn(@Valid @RequestBody ClockInRequest request) {
        TimeEntryEntity entry = timeTrackingService.clockIn(request.employeeId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(TimeEntryResponse.from(entry)));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> clockOut(@Valid @RequestBody ClockOutRequest request) {
        TimeEntryEntity entry = timeTrackingService.clockOut(request.employeeId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(TimeEntryResponse.from(entry)));
    }

    @PostMapping("/job-start")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> jobStart(@Valid @RequestBody JobTrackingRequest request) {
        TimeEntryEntity entry = timeTrackingService.startJob(request.employeeId(), request.jobId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(TimeEntryResponse.from(entry)));
    }

    @PostMapping("/job-end")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> jobEnd(@Valid @RequestBody JobTrackingRequest request) {
        TimeEntryEntity entry = timeTrackingService.endJob(request.employeeId(), request.jobId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(TimeEntryResponse.from(entry)));
    }

    @PostMapping("/{id}/correct")
    public ResponseEntity<ApiResponse<TimeCorrectionResponse>> correct(
            @PathVariable UUID id, @Valid @RequestBody TimeCorrectionRequest request) {
        TimeCorrectionEntity correction = timeTrackingService.correctEntry(
                id, request.newTimestamp(), request.correctedBy(), request.reason());
        return ResponseEntity.ok(ApiResponse.ok(TimeCorrectionResponse.from(correction)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TimeEntryResponse>>> getEntries(
            @RequestParam UUID employeeId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            Pageable pageable) {
        Page<TimeEntryResponse> page = timeTrackingService.getEntries(employeeId, from, to, pageable)
                .map(TimeEntryResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }
}
