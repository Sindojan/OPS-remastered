package com.sindoflow.ops.bom.dto;

import com.sindoflow.ops.bom.PartEntity;
import com.sindoflow.ops.bom.PartType;

import java.time.Instant;
import java.util.UUID;

public record PartResponse(
        UUID id,
        String partNumber,
        String name,
        String description,
        PartType type,
        UUID unitId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static PartResponse from(PartEntity e) {
        return new PartResponse(
                e.getId(), e.getPartNumber(), e.getName(), e.getDescription(),
                e.getType(), e.getUnitId(), e.getStatus(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
