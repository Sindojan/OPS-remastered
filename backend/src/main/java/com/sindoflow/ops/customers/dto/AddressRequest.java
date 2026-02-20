package com.sindoflow.ops.customers.dto;

import com.sindoflow.ops.customers.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressRequest(
        @NotNull AddressType type,
        @NotBlank String street,
        @NotBlank String zip,
        @NotBlank String city,
        String country
) {}
