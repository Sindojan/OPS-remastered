package com.sindoflow.ops.events.dto;

import com.sindoflow.ops.events.ScheduledTriggerEntity;

import java.time.Instant;
import java.util.UUID;

public record ScheduledTriggerResponse(
        UUID id,
        UUID instanceId,
        String cronExpression,
        Instant lastRunAt,
        Instant nextRunAt,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static ScheduledTriggerResponse from(ScheduledTriggerEntity entity) {
        return new ScheduledTriggerResponse(
                entity.getId(),
                entity.getInstanceId(),
                entity.getCronExpression(),
                entity.getLastRunAt(),
                entity.getNextRunAt(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
