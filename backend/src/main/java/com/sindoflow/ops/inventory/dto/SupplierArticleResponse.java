package com.sindoflow.ops.inventory.dto;

import com.sindoflow.ops.inventory.SupplierArticleEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SupplierArticleResponse(
        UUID id,
        UUID supplierId,
        UUID articleId,
        String supplierArticleNumber,
        Integer leadTimeDays,
        BigDecimal minOrderQuantity,
        Instant createdAt
) {
    public static SupplierArticleResponse from(SupplierArticleEntity e) {
        return new SupplierArticleResponse(
                e.getId(), e.getSupplierId(), e.getArticleId(),
                e.getSupplierArticleNumber(), e.getLeadTimeDays(),
                e.getMinOrderQuantity(), e.getCreatedAt()
        );
    }
}
