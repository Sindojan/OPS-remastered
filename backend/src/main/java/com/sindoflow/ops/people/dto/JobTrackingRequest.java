package com.sindoflow.ops.people.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record JobTrackingRequest(
        @NotNull UUID employeeId,
        @NotNull UUID jobId
) {}
