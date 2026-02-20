package com.sindoflow.ops.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierArticleRequest(
        @NotNull UUID articleId,
        String supplierArticleNumber,
        Integer leadTimeDays,
        BigDecimal minOrderQuantity
) {}
