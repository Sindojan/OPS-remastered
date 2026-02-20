package com.sindoflow.ops.people.dto;

import com.sindoflow.ops.people.EmployeeEntity;
import com.sindoflow.ops.people.EmployeeStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        UUID userId,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        EmployeeStatus status,
        LocalDate hireDate,
        UUID stationId,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmployeeResponse from(EmployeeEntity e) {
        return new EmployeeResponse(
                e.getId(), e.getUserId(), e.getEmployeeNumber(),
                e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(),
                e.getRole(), e.getStatus(), e.getHireDate(), e.getStationId(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
