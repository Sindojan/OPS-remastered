package com.owlsburg.ops.agentinfra.dto;

import com.owlsburg.ops.agentinfra.AgentRunStepEntity;
import com.owlsburg.ops.agentinfra.AgentStepType;

import java.time.Instant;
import java.util.UUID;

public record AgentRunStepResponse(
        UUID id,
        UUID runId,
        int stepNumber,
        AgentStepType type,
        String toolName,
        String input,
        String output,
        int tokensUsed,
        Integer durationMs,
        Instant createdAt
) {
    public static AgentRunStepResponse from(AgentRunStepEntity entity) {
        return new AgentRunStepResponse(
                entity.getId(),
                entity.getRunId(),
                entity.getStepNumber(),
                entity.getType(),
                entity.getToolName(),
                entity.getInput(),
                entity.getOutput(),
                entity.getTokensUsed(),
                entity.getDurationMs(),
                entity.getCreatedAt()
        );
    }
}
