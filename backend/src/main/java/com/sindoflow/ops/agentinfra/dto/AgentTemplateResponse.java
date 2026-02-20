package com.sindoflow.ops.agentinfra.dto;

import com.sindoflow.ops.agentinfra.AgentTemplateEntity;

import java.time.Instant;
import java.util.UUID;

public record AgentTemplateResponse(
        UUID id,
        String name,
        String role,
        String description,
        String basePrompt,
        String allowedTools,
        String triggerTypes,
        int maxTokensPerRun,
        int dailyTokenBudget,
        String status,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static AgentTemplateResponse from(AgentTemplateEntity entity) {
        return new AgentTemplateResponse(
                entity.getId(),
                entity.getName(),
                entity.getRole(),
                entity.getDescription(),
                entity.getBasePrompt(),
                entity.getAllowedTools(),
                entity.getTriggerTypes(),
                entity.getMaxTokensPerRun(),
                entity.getDailyTokenBudget(),
                entity.getStatus(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
