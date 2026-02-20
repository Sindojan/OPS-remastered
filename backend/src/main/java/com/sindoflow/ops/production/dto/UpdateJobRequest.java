package com.sindoflow.ops.production.dto;

import java.time.Instant;
import java.util.UUID;

public record UpdateJobRequest(
        UUID customerId,
        String title,
        Integer priority,
        Integer quantity,
        Instant deadline,
        String notes
) {}
