package com.sindoflow.ops.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TenantCreateRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "Slug must be lowercase alphanumeric with underscores") String slug
) {}
