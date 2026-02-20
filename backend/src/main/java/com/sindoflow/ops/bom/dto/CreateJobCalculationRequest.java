package com.sindoflow.ops.bom.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateJobCalculationRequest(
        @NotNull UUID jobId,
        @NotNull UUID calculationId
) {}
