package com.owlsburg.ops.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TenantCreateRequest(
        @NotBlank String name,
        @NotBlank @Email String adminEmail,
        @NotBlank String adminPassword
) {}
