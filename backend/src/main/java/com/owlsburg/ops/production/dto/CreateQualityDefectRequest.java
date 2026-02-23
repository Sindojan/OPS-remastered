package com.owlsburg.ops.production.dto;

import com.owlsburg.ops.common.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQualityDefectRequest(
        @NotBlank String defectType,
        String description,
        @NotNull SeverityLevel severity
) {}
