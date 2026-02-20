package com.sindoflow.ops.agentinfra.dto;

import com.sindoflow.ops.agentinfra.AgentStepType;
import jakarta.validation.constraints.NotNull;

public record AddAgentRunStepRequest(
        @NotNull AgentStepType type,
        String toolName,
        String input,
        String output,
        int tokensUsed,
        int durationMs
) {}
