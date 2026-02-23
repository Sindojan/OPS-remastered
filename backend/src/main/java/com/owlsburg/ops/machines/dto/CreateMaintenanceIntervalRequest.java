package com.owlsburg.ops.machines.dto;

import com.owlsburg.ops.machines.MaintenanceType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateMaintenanceIntervalRequest(
        @NotNull UUID machineId,
        @NotNull MaintenanceType type,
        Integer intervalDays,
        Integer intervalHours,
        Instant nextDueAt,
        String description
) {}
