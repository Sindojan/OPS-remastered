package com.owlsburg.ops.agentinfra.dto;

import com.owlsburg.ops.agentinfra.AgentInstanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAgentInstanceRequest(
        @NotNull UUID templateId,
        @NotBlank String name,
        UUID parentInstanceId,
        @NotNull AgentInstanceType type,
        UUID tenantId,
        String config
) {}
