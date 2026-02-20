package com.sindoflow.ops.production.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        List<Integer> daysOfWeek,
        BigDecimal capacityHours,
        Instant createdAt,
        Instant updatedAt
) {}
