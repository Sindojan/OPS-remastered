package com.owlsburg.ops.systemagent.dto;

import com.owlsburg.ops.systemagent.SystemChatSessionEntity;

import java.time.Instant;
import java.util.UUID;

public record SystemChatSessionResponse(
        UUID id,
        String title,
        UUID agentInstanceId,
        Instant createdAt,
        Instant updatedAt
) {
    public static SystemChatSessionResponse from(SystemChatSessionEntity entity) {
        return new SystemChatSessionResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getAgentInstanceId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
