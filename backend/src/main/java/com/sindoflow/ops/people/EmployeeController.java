package com.sindoflow.ops.people;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.people.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final TimeTrackingService timeTrackingService;

    public EmployeeController(EmployeeService employeeService,
                              TimeTrackingService timeTrackingService) {
        this.employeeService = employeeService;
        this.timeTrackingService = timeTrackingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeEntity entity = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(EmployeeResponse.from(entity)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable UUID id) {
        EmployeeEntity entity = employeeService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(EmployeeResponse.from(entity)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> findAll(Pageable pageable) {
        Page<EmployeeResponse> page = employeeService.findAll(pageable).map(EmployeeResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateEmployeeRequest request) {
        EmployeeEntity entity = employeeService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(EmployeeResponse.from(entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Employee deleted"));
    }

    @GetMapping("/by-station/{stationId}")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getByStation(@PathVariable UUID stationId) {
        List<EmployeeResponse> list = employeeService.getByStation(stationId).stream()
                .map(EmployeeResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/{id}/qualifications")
    public ResponseEntity<ApiResponse<QualificationResponse>> addQualification(
            @PathVariable UUID id, @Valid @RequestBody QualificationRequest request) {
        EmployeeQualificationEntity entity = employeeService.addQualification(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(QualificationResponse.from(entity)));
    }

    @GetMapping("/{id}/qualifications")
    public ResponseEntity<ApiResponse<List<QualificationResponse>>> getQualifications(@PathVariable UUID id) {
        List<QualificationResponse> list = employeeService.getQualifications(id).stream()
                .map(QualificationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @DeleteMapping("/{id}/qualifications/{qId}")
    public ResponseEntity<ApiResponse<Void>> removeQualification(@PathVariable UUID id, @PathVariable UUID qId) {
        employeeService.removeQualification(id, qId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Qualification removed"));
    }

    @GetMapping("/{id}/my-day")
    public ResponseEntity<ApiResponse<MyDayResponse>> getMyDay(@PathVariable UUID id) {
        MyDayResponse response = timeTrackingService.getMyDay(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
