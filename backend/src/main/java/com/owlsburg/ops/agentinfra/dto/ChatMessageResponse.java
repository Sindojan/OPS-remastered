package com.owlsburg.ops.agentinfra.dto;

import com.owlsburg.ops.agentinfra.ChatMessageEntity;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        String role,
        String content,
        Instant createdAt
) {
    public static ChatMessageResponse from(ChatMessageEntity entity) {
        return new ChatMessageResponse(
                entity.getId(),
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
