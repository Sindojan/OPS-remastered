package com.sindoflow.ops.agentinfra.dto;

import com.sindoflow.ops.agentinfra.AgentInstanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAgentInstanceRequest(
        @NotNull UUID templateId,
        @NotBlank String name,
        UUID parentInstanceId,
        @NotNull AgentInstanceType type,
        String tenantId,
        String config
) {}
