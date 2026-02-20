package com.sindoflow.ops.bom.dto;

import com.sindoflow.ops.bom.BomItemEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record BomItemResponse(
        UUID id,
        UUID bomVersionId,
        UUID componentPartId,
        BigDecimal quantity,
        UUID unitId,
        int position,
        String notes
) {
    public static BomItemResponse from(BomItemEntity e) {
        return new BomItemResponse(
                e.getId(), e.getBomVersionId(), e.getComponentPartId(),
                e.getQuantity(), e.getUnitId(), e.getPosition(), e.getNotes()
        );
    }
}
