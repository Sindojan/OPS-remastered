package com.sindoflow.ops.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMovementRequest(
        @NotNull UUID articleId,
        UUID fromLocationId,
        UUID toLocationId,
        @NotNull BigDecimal quantity,
        @NotNull String type,
        String referenceType,
        UUID referenceId,
        UUID performedBy,
        String notes
) {}
