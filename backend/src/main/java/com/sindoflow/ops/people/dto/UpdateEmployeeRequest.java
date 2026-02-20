package com.sindoflow.ops.people.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateEmployeeRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        String status,
        LocalDate hireDate,
        UUID stationId
) {}
