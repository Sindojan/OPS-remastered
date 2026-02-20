package com.sindoflow.ops.inbox.dto;

import com.sindoflow.ops.inbox.ConversationMessageEntity;
import com.sindoflow.ops.inbox.SenderType;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        String content,
        SenderType senderType,
        UUID senderId,
        Instant sentAt
) {
    public static MessageResponse from(ConversationMessageEntity entity) {
        return new MessageResponse(
                entity.getId(),
                entity.getConversation().getId(),
                entity.getContent(),
                entity.getSenderType(),
                entity.getSenderId(),
                entity.getSentAt()
        );
    }
}
