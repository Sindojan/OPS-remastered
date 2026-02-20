package com.sindoflow.ops.bom.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BomItemRequest(
        @NotNull UUID componentPartId,
        @NotNull BigDecimal quantity,
        UUID unitId,
        int position,
        String notes
) {}
