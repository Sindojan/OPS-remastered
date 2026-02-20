package com.sindoflow.ops.inbox.dto;

import jakarta.validation.constraints.NotBlank;

public record TagRequest(
        @NotBlank String tag
) {}
