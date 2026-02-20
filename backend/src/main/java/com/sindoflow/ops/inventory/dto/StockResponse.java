package com.sindoflow.ops.inventory.dto;

import com.sindoflow.ops.inventory.StockEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockResponse(
        UUID id,
        UUID articleId,
        UUID warehouseLocationId,
        BigDecimal quantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        Instant updatedAt
) {
    public static StockResponse from(StockEntity e) {
        return new StockResponse(
                e.getId(), e.getArticleId(), e.getWarehouseLocationId(),
                e.getQuantity(), e.getReservedQuantity(),
                e.getQuantity().subtract(e.getReservedQuantity()),
                e.getUpdatedAt()
        );
    }
}
