package com.owlsburg.ops.agentinfra.dto;

import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentInstanceStatus;
import com.owlsburg.ops.agentinfra.AgentInstanceType;

import java.time.Instant;
import java.util.UUID;

public record AgentInstanceResponse(
        UUID id,
        UUID templateId,
        String name,
        UUID parentInstanceId,
        AgentInstanceType type,
        AgentInstanceStatus status,
        UUID tenantId,
        String config,
        String customSystemPrompt,
        Instant terminatedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AgentInstanceResponse from(AgentInstanceEntity entity) {
        return new AgentInstanceResponse(
                entity.getId(),
                entity.getTemplateId(),
                entity.getName(),
                entity.getParentInstanceId(),
                entity.getType(),
                entity.getStatus(),
                entity.getTenantId(),
                entity.getConfig(),
                entity.getCustomSystemPrompt(),
                entity.getTerminatedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
