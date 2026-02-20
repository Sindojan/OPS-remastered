package com.sindoflow.ops.customers.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerCreateRequest(
        @NotBlank String companyName,
        String taxId
) {}
