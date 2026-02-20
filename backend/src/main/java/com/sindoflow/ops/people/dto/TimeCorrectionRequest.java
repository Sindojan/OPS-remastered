package com.sindoflow.ops.people.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record TimeCorrectionRequest(
        @NotNull Instant newTimestamp,
        @NotNull UUID correctedBy,
        @NotBlank String reason
) {}
