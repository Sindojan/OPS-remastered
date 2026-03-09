package com.owlsburg.ops.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CriticalArticleResponse(
        UUID articleId,
        String articleName,
        String articleNumber,
        UUID warehouseLocationId,
        BigDecimal currentQuantity,
        BigDecimal minStock,
        BigDecimal deficit
) {}
