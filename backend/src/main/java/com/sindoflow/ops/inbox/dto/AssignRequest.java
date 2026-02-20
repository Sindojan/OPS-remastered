package com.sindoflow.ops.inbox.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignRequest(
        @NotNull UUID assignedTo
) {}
