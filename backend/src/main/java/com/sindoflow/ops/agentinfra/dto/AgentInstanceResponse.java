package com.sindoflow.ops.agentinfra.dto;

import com.sindoflow.ops.agentinfra.AgentInstanceEntity;
import com.sindoflow.ops.agentinfra.AgentInstanceStatus;
import com.sindoflow.ops.agentinfra.AgentInstanceType;

import java.time.Instant;
import java.util.UUID;

public record AgentInstanceResponse(
        UUID id,
        UUID templateId,
        String name,
        UUID parentInstanceId,
        AgentInstanceType type,
        AgentInstanceStatus status,
        String tenantId,
        String config,
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
                entity.getTerminatedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
