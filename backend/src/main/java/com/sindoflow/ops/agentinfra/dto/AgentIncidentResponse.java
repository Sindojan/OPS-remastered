package com.sindoflow.ops.agentinfra.dto;

import com.sindoflow.ops.agentinfra.AgentIncidentEntity;

import java.time.Instant;
import java.util.UUID;

public record AgentIncidentResponse(
        UUID id,
        UUID instanceId,
        String type,
        String description,
        Instant createdAt,
        Instant resolvedAt
) {
    public static AgentIncidentResponse from(AgentIncidentEntity entity) {
        return new AgentIncidentResponse(
                entity.getId(),
                entity.getInstanceId(),
                entity.getType(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getResolvedAt()
        );
    }
}
