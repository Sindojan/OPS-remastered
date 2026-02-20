package com.sindoflow.ops.bom.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBomVersionRequest(
        @NotNull UUID partId,
        int versionNumber,
        LocalDate validFrom,
        UUID createdBy
) {}
