package com.sindoflow.ops.customers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceGroupRequest(
        @NotBlank String name,
        @NotNull BigDecimal discountPercent,
        @NotNull LocalDate validFrom,
        LocalDate validUntil
) {}
