package com.sindoflow.ops.customers.dto;

import jakarta.validation.constraints.NotBlank;

public record ContactRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String email,
        String phone,
        String position,
        Boolean isPrimary
) {}
