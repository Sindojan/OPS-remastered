package com.sindoflow.ops.production.dto;

import com.sindoflow.ops.production.JobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String jobNumber,
        UUID customerId,
        String title,
        JobStatus status,
        int priority,
        int quantity,
        Instant deadline,
        String notes,
        UUID createdBy,
        UUID assignedStationId,
        UUID shiftId,
        Instant startedAt,
        Instant completedAt,
        List<JobStatusHistoryResponse> statusHistory,
        Instant createdAt,
        Instant updatedAt
) {}
