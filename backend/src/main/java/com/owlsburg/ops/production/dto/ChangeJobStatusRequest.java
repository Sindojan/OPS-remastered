package com.owlsburg.ops.production.dto;

import com.owlsburg.ops.production.JobStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeJobStatusRequest(
        @NotNull JobStatus newStatus,
        UUID changedBy,
        String reason
) {}
