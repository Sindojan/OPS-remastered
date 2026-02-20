package com.sindoflow.ops.bom.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinalizeJobCalculationRequest(
        @NotNull BigDecimal actualMaterialCost,
        @NotNull BigDecimal actualLaborCost
) {}
