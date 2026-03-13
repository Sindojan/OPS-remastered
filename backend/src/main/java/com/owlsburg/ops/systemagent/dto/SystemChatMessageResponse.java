package com.owlsburg.ops.systemagent.dto;

import com.owlsburg.ops.systemagent.SystemChatMessageEntity;

import java.time.Instant;
import java.util.UUID;

public record SystemChatMessageResponse(
        UUID id,
        String role,
        String content,
        Instant createdAt
) {
    public static SystemChatMessageResponse from(SystemChatMessageEntity entity) {
        return new SystemChatMessageResponse(
                entity.getId(),
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
