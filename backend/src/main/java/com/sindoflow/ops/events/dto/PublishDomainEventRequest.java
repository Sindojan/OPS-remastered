package com.sindoflow.ops.events.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record PublishDomainEventRequest(
        @NotBlank String eventType,
        @NotBlank String sourceType,
        UUID sourceId,
        String payload
) {}
