package com.sindoflow.ops.machines.dto;

import com.sindoflow.ops.machines.MaintenanceRecordStatus;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceRecordResponse(
        UUID id,
        UUID machineId,
        UUID intervalId,
        UUID performedBy,
        Instant performedAt,
        Integer durationMinutes,
        String notes,
        MaintenanceRecordStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
