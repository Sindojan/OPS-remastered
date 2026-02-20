package com.sindoflow.ops.bom.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ProcessStepRequest(
        int stepNumber,
        @NotBlank String name,
        String description,
        UUID stationId,
        UUID machineId,
        int setupTimeMinutes,
        int processingTimeMinutes,
        String notes
) {}
