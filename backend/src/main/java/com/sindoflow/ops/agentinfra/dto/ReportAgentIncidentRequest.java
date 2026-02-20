package com.sindoflow.ops.agentinfra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReportAgentIncidentRequest(
        @NotNull UUID instanceId,
        @NotBlank String type,
        String description
) {}
