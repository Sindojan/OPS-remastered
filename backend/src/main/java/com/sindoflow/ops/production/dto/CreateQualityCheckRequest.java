package com.sindoflow.ops.production.dto;

import com.sindoflow.ops.production.QualityResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateQualityCheckRequest(
        @NotNull UUID jobId,
        UUID checkedBy,
        @NotBlank String checkType,
        @NotNull QualityResult result,
        int defectCount,
        String notes
) {}
