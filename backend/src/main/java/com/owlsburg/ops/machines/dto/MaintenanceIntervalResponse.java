package com.owlsburg.ops.machines.dto;

import com.owlsburg.ops.machines.MaintenanceType;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceIntervalResponse(
        UUID id,
        UUID machineId,
        MaintenanceType type,
        Integer intervalDays,
        Integer intervalHours,
        Instant lastPerformedAt,
        Instant nextDueAt,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
