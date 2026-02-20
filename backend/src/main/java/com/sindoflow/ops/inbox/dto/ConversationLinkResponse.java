package com.sindoflow.ops.inbox.dto;

import com.sindoflow.ops.inbox.ConversationLinkEntity;

import java.time.Instant;
import java.util.UUID;

public record ConversationLinkResponse(
        UUID id,
        UUID conversationId,
        String linkedType,
        UUID linkedId,
        Instant createdAt
) {
    public static ConversationLinkResponse from(ConversationLinkEntity entity) {
        return new ConversationLinkResponse(
                entity.getId(),
                entity.getConversationId(),
                entity.getLinkedType(),
                entity.getLinkedId(),
                entity.getCreatedAt()
        );
    }
}
