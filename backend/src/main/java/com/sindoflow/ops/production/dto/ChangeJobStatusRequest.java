package com.sindoflow.ops.production.dto;

import com.sindoflow.ops.production.JobStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeJobStatusRequest(
        @NotNull JobStatus newStatus,
        UUID changedBy,
        String reason
) {}
