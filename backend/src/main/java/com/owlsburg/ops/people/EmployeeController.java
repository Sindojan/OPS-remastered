package com.owlsburg.ops.people;

import com.owlsburg.ops.auth.Role;
import com.owlsburg.ops.auth.UserEntity;
import com.owlsburg.ops.auth.UserService;
import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.people.dto.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;
    private final TimeTrackingService timeTrackingService;
    private final UserService userService;

    public EmployeeController(EmployeeService employeeService,
                              TimeTrackingService timeTrackingService,
                              UserService userService) {
        this.employeeService = employeeService;
        this.timeTrackingService = timeTrackingService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeCreateResponse>> create(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeCreateResponse.UserCredentials credentials = null;

        // Auto-create user account if email and systemRole are provided
        UUID userId = request.userId();
        if (userId == null && request.email() != null && !request.email().isBlank()
                && request.systemRole() != null && !request.systemRole().isBlank()) {
            Role role = Role.valueOf(request.systemRole());
            String password = request.password() != null && !request.password().isBlank()
                    ? request.password()
                    : UUID.randomUUID().toString().substring(0, 12);
            UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());

            UserEntity user = userService.create(
                    request.email(), password, request.firstName(), request.lastName(), role, tenantId
            );
            userId = user.getId();
            credentials = new EmployeeCreateResponse.UserCredentials(
                    user.getId(), user.getEmail(), role.name(), password
            );
            log.info("Auto-created user {} with role {} for employee {}", user.getEmail(), role, request.employeeNumber());
        }

        // Create employee with linked userId
        CreateEmployeeRequest withUser = new CreateEmployeeRequest(
                userId, request.employeeNumber(), request.firstName(), request.lastName(),
                request.email(), request.phone(), request.role(), request.systemRole(),
                null, request.hireDate(), request.stationId()
        );
        EmployeeEntity entity = employeeService.create(withUser);

        EmployeeCreateResponse response = new EmployeeCreateResponse(
                EmployeeResponse.from(entity), credentials
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
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

        // If systemRole changed and employee has a linked user, update user role
        if (request.systemRole() != null && !request.systemRole().isBlank() && entity.getUserId() != null) {
            try {
                Role newRole = Role.valueOf(request.systemRole());
                userService.updateRole(entity.getUserId(), newRole);
                log.info("Updated system role to {} for user {} (employee {})", newRole, entity.getUserId(), id);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid system role: {}", request.systemRole());
            }
        }

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
