package com.owlsburg.ops.inbox.dto;

import jakarta.validation.constraints.NotBlank;

public record TagRequest(
        @NotBlank String tag
) {}
