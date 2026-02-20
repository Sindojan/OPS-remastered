package com.sindoflow.ops.people.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ShiftAssignmentRequest(
        @NotNull UUID shiftId,
        @NotNull LocalDate validFrom,
        LocalDate validUntil
) {}
