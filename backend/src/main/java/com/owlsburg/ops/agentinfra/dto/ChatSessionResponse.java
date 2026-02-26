package com.owlsburg.ops.agentinfra.dto;

import com.owlsburg.ops.agentinfra.ChatSessionEntity;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionResponse(
        UUID id,
        String title,
        UUID agentInstanceId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ChatSessionResponse from(ChatSessionEntity entity) {
        return new ChatSessionResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getAgentInstanceId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
