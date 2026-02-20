package com.sindoflow.ops.production.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public record CreateJobRequest(
        @NotBlank String jobNumber,
        UUID customerId,
        @NotBlank String title,
        int priority,
        int quantity,
        Instant deadline,
        String notes,
        UUID createdBy
) {}
