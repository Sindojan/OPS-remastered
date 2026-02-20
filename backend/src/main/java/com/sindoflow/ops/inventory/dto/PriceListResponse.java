package com.sindoflow.ops.inventory.dto;

import com.sindoflow.ops.inventory.SupplierPriceListEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PriceListResponse(
        UUID id,
        UUID supplierArticleId,
        BigDecimal price,
        String currency,
        LocalDate validFrom,
        LocalDate validUntil,
        Instant createdAt
) {
    public static PriceListResponse from(SupplierPriceListEntity e) {
        return new PriceListResponse(
                e.getId(), e.getSupplierArticleId(), e.getPrice(),
                e.getCurrency(), e.getValidFrom(), e.getValidUntil(), e.getCreatedAt()
        );
    }
}
