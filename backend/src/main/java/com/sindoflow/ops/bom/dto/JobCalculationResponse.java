package com.sindoflow.ops.bom.dto;

import com.sindoflow.ops.bom.JobCalculationEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JobCalculationResponse(
        UUID id,
        UUID jobId,
        UUID calculationId,
        BigDecimal actualMaterialCost,
        BigDecimal actualLaborCost,
        BigDecimal actualTotalCost,
        BigDecimal variancePercent,
        Instant finalizedAt
) {
    public static JobCalculationResponse from(JobCalculationEntity e) {
        return new JobCalculationResponse(
                e.getId(), e.getJobId(), e.getCalculationId(),
                e.getActualMaterialCost(), e.getActualLaborCost(),
                e.getActualTotalCost(), e.getVariancePercent(), e.getFinalizedAt()
        );
    }
}
