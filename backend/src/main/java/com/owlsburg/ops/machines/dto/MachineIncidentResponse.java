package com.owlsburg.ops.machines.dto;

import com.owlsburg.ops.common.SeverityLevel;

import java.time.Instant;
import java.util.UUID;

public record MachineIncidentResponse(
        UUID id,
        UUID machineId,
        UUID reportedBy,
        String type,
        String description,
        SeverityLevel severity,
        Instant reportedAt,
        Instant resolvedAt,
        String resolutionNotes
) {}
