package com.owlsburg.ops.auth.dto;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull Boolean active
) {}
