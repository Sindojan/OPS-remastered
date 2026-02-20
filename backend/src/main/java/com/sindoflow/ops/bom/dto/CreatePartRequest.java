package com.sindoflow.ops.bom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePartRequest(
        @NotBlank String partNumber,
        @NotBlank String name,
        String description,
        @NotNull String type,
        UUID unitId,
        String status
) {}
