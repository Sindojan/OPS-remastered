package com.sindoflow.ops.inventory.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StockSummaryResponse(
        UUID articleId,
        BigDecimal totalQuantity,
        BigDecimal totalReserved,
        BigDecimal totalAvailable,
        List<StockResponse> locations
) {}
