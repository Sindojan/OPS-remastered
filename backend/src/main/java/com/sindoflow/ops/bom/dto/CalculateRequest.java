package com.sindoflow.ops.bom.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CalculateRequest(
        @NotNull UUID partId,
        @NotNull UUID bomVersionId,
        @NotNull UUID processPlanId,
        int quantity,
        UUID calculatedBy
) {}
