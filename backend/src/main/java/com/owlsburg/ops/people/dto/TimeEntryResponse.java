package com.owlsburg.ops.people.dto;

import com.owlsburg.ops.people.TimeEntryEntity;
import com.owlsburg.ops.people.TimeEntryType;

import java.time.Instant;
import java.util.UUID;

public record TimeEntryResponse(
        UUID id,
        UUID employeeId,
        TimeEntryType type,
        UUID jobId,
        Instant timestamp,
        Instant createdAt
) {
    public static TimeEntryResponse from(TimeEntryEntity e) {
        return new TimeEntryResponse(
                e.getId(), e.getEmployeeId(), e.getType(),
                e.getJobId(), e.getTimestamp(), e.getCreatedAt()
        );
    }
}
