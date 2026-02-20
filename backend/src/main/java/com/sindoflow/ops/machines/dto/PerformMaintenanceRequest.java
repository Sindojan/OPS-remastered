package com.sindoflow.ops.machines.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PerformMaintenanceRequest(
        @NotNull UUID machineId,
        UUID intervalId,
        @NotNull UUID performedBy,
        int durationMinutes,
        String notes
) {}
