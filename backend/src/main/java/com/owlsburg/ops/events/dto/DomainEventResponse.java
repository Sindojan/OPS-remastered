package com.owlsburg.ops.events.dto;

import com.owlsburg.ops.events.DomainEventEntity;

import java.time.Instant;
import java.util.UUID;

public record DomainEventResponse(
        UUID id,
        String eventType,
        String sourceType,
        UUID sourceId,
        String payload,
        boolean processed,
        Instant createdAt
) {
    public static DomainEventResponse from(DomainEventEntity entity) {
        return new DomainEventResponse(
                entity.getId(),
                entity.getEventType(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getPayload(),
                entity.isProcessed(),
                entity.getCreatedAt()
        );
    }
}
