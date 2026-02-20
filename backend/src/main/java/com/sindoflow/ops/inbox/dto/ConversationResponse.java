package com.sindoflow.ops.inbox.dto;

import com.sindoflow.ops.inbox.*;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String subject,
        UUID customerId,
        ConversationStatus status,
        ConversationPriority priority,
        Instant slaDueAt,
        UUID assignedTo,
        ConversationSource source,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConversationResponse from(ConversationEntity entity) {
        return new ConversationResponse(
                entity.getId(),
                entity.getSubject(),
                entity.getCustomerId(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getSlaDueAt(),
                entity.getAssignedTo(),
                entity.getSource(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
