package com.sindoflow.ops.inventory.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateArticleRequest(
        @NotBlank String articleNumber,
        @NotBlank String name,
        String description,
        UUID categoryId,
        UUID unitId,
        BigDecimal minStock,
        BigDecimal reorderPoint,
        String status
) {}
