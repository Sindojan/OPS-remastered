package com.owlsburg.ops.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public record SuspendRequest(@NotBlank String reason) {}
