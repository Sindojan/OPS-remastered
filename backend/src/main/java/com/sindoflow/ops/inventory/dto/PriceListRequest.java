package com.sindoflow.ops.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceListRequest(
        @NotNull BigDecimal price,
        String currency,
        @NotNull LocalDate validFrom,
        LocalDate validUntil
) {}
