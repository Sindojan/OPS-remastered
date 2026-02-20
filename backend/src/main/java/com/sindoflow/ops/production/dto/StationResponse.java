package com.sindoflow.ops.production.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StationResponse(
        UUID id,
        String name,
        String description,
        Integer capacityPerShift,
        String status,
        int totalCapacity,
        List<UUID> shiftIds,
        Instant createdAt,
        Instant updatedAt
) {}
