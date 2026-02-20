package com.sindoflow.ops.production.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignJobRequest(
        @NotNull UUID stationId,
        @NotNull UUID shiftId
) {}
