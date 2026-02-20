package com.sindoflow.ops.auth.dto;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull Boolean active
) {}
