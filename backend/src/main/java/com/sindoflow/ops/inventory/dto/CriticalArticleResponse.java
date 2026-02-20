package com.sindoflow.ops.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CriticalArticleResponse(
        UUID articleId,
        UUID warehouseLocationId,
        BigDecimal currentQuantity,
        BigDecimal minStock,
        BigDecimal deficit
) {}
