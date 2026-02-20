package com.sindoflow.ops.production.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStationRequest(
        @NotBlank String name,
        String description,
        Integer capacityPerShift,
        String status
) {}
