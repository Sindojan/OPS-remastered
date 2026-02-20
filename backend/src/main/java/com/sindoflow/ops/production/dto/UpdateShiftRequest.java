package com.sindoflow.ops.production.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record UpdateShiftRequest(
        String name,
        LocalTime startTime,
        LocalTime endTime,
        List<Integer> daysOfWeek,
        BigDecimal capacityHours
) {}
