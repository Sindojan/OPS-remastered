package com.sindoflow.ops.machines.dto;

import com.sindoflow.ops.machines.MaintenanceType;
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
