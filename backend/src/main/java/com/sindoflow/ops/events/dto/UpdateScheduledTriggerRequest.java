package com.sindoflow.ops.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateScheduledTriggerRequest(
        @NotNull UUID instanceId,
        @NotBlank String cronExpression
) {}
