package com.sindoflow.ops.bom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateProcessPlanRequest(
        @NotNull UUID partId,
        int versionNumber,
        @NotBlank String name,
        LocalDate validFrom,
        UUID createdBy
) {}
