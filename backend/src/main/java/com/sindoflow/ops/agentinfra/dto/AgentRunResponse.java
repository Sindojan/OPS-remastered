package com.sindoflow.ops.agentinfra.dto;

import com.sindoflow.ops.agentinfra.AgentRunEntity;
import com.sindoflow.ops.agentinfra.AgentRunStatus;
import com.sindoflow.ops.agentinfra.TriggerType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentRunResponse(
        UUID id,
        UUID instanceId,
        TriggerType triggerType,
        String triggerSource,
        String inputContext,
        String output,
        AgentRunStatus status,
        int tokensUsed,
        BigDecimal costUsd,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        Instant createdAt,
        List<AgentRunStepResponse> steps
) {
    public static AgentRunResponse from(AgentRunEntity entity) {
        return from(entity, null);
    }

    public static AgentRunResponse from(AgentRunEntity entity, List<AgentRunStepResponse> steps) {
        return new AgentRunResponse(
                entity.getId(),
                entity.getInstanceId(),
                entity.getTriggerType(),
                entity.getTriggerSource(),
                entity.getInputContext(),
                entity.getOutput(),
                entity.getStatus(),
                entity.getTokensUsed(),
                entity.getCostUsd(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                steps
        );
    }
}
