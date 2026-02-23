package com.owlsburg.ops.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateArticleRequest(
        String name,
        String description,
        UUID categoryId,
        UUID unitId,
        BigDecimal minStock,
        BigDecimal reorderPoint,
        String status
) {}
