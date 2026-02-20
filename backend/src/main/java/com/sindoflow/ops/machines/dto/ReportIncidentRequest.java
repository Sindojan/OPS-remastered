package com.sindoflow.ops.machines.dto;

import com.sindoflow.ops.common.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReportIncidentRequest(
        UUID reportedBy,
        @NotBlank String type,
        String description,
        @NotNull SeverityLevel severity
) {}
