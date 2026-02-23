package com.owlsburg.ops.production.dto;

import com.owlsburg.ops.production.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobStatusHistoryResponse(
        UUID id,
        JobStatus fromStatus,
        JobStatus toStatus,
        UUID changedBy,
        Instant changedAt,
        String reason
) {}
