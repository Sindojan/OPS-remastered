package com.sindoflow.ops.people.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAbsenceRequest(
        @NotNull UUID employeeId,
        @NotNull String type,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        String notes
) {}
