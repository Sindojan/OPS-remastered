package com.owlsburg.ops.agentinfra.dto;

import com.owlsburg.ops.agentinfra.AgentStepType;
import jakarta.validation.constraints.NotNull;

public record AddAgentRunStepRequest(
        @NotNull AgentStepType type,
        String toolName,
        String input,
        String output,
        int tokensUsed,
        int durationMs
) {}
