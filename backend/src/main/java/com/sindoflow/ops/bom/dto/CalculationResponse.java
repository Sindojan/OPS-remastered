package com.sindoflow.ops.bom.dto;

import com.sindoflow.ops.bom.CalculationEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CalculationResponse(
        UUID id,
        UUID partId,
        UUID bomVersionId,
        UUID processPlanId,
        int quantity,
        BigDecimal materialCost,
        BigDecimal laborCost,
        BigDecimal overheadCost,
        BigDecimal totalCost,
        String currency,
        Instant calculatedAt,
        UUID calculatedBy
) {
    public static CalculationResponse from(CalculationEntity e) {
        return new CalculationResponse(
                e.getId(), e.getPartId(), e.getBomVersionId(), e.getProcessPlanId(),
                e.getQuantity(), e.getMaterialCost(), e.getLaborCost(),
                e.getOverheadCost(), e.getTotalCost(), e.getCurrency(),
                e.getCalculatedAt(), e.getCalculatedBy()
        );
    }
}
