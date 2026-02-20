package com.sindoflow.ops.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record CreateShiftRequest(
        @NotBlank String name,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull List<Integer> daysOfWeek,
        BigDecimal capacityHours
) {}
