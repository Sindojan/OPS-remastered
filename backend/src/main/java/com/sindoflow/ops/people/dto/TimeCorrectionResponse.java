package com.sindoflow.ops.people.dto;

import com.sindoflow.ops.people.TimeCorrectionEntity;

import java.time.Instant;
import java.util.UUID;

public record TimeCorrectionResponse(
        UUID id,
        UUID timeEntryId,
        UUID correctedBy,
        Instant oldTimestamp,
        Instant newTimestamp,
        String reason,
        Instant createdAt
) {
    public static TimeCorrectionResponse from(TimeCorrectionEntity e) {
        return new TimeCorrectionResponse(
                e.getId(), e.getTimeEntryId(), e.getCorrectedBy(),
                e.getOldTimestamp(), e.getNewTimestamp(), e.getReason(),
                e.getCreatedAt()
        );
    }
}
