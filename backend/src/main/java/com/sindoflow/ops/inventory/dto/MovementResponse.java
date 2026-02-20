package com.sindoflow.ops.inventory.dto;

import com.sindoflow.ops.inventory.StockMovementEntity;
import com.sindoflow.ops.inventory.StockMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
        UUID id,
        UUID articleId,
        UUID fromLocationId,
        UUID toLocationId,
        BigDecimal quantity,
        StockMovementType type,
        String referenceType,
        UUID referenceId,
        UUID performedBy,
        String notes,
        Instant createdAt
) {
    public static MovementResponse from(StockMovementEntity e) {
        return new MovementResponse(
                e.getId(), e.getArticleId(), e.getFromLocationId(), e.getToLocationId(),
                e.getQuantity(), e.getType(), e.getReferenceType(), e.getReferenceId(),
                e.getPerformedBy(), e.getNotes(), e.getCreatedAt()
        );
    }
}
