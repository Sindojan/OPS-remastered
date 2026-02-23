package com.owlsburg.ops.customers.dto;

import com.owlsburg.ops.customers.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressRequest(
        @NotNull AddressType type,
        @NotBlank String street,
        @NotBlank String zip,
        @NotBlank String city,
        String country
) {}
