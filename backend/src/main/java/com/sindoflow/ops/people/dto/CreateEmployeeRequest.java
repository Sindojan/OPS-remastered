package com.sindoflow.ops.people.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEmployeeRequest(
        UUID userId,
        @NotBlank String employeeNumber,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String email,
        String phone,
        String role,
        LocalDate hireDate,
        UUID stationId
) {}
