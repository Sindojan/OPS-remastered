package com.owlsburg.ops.machines.dto;

import com.owlsburg.ops.common.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReportIncidentRequest(
        UUID reportedBy,
        @NotBlank String type,
        String description,
        @NotNull SeverityLevel severity
) {}
