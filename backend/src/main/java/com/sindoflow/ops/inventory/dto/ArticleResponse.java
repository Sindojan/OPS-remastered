package com.sindoflow.ops.inventory.dto;

import com.sindoflow.ops.inventory.ArticleEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String articleNumber,
        String name,
        String description,
        UUID categoryId,
        UUID unitId,
        BigDecimal minStock,
        BigDecimal reorderPoint,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ArticleResponse from(ArticleEntity e) {
        return new ArticleResponse(
                e.getId(), e.getArticleNumber(), e.getName(), e.getDescription(),
                e.getCategoryId(), e.getUnitId(), e.getMinStock(), e.getReorderPoint(),
                e.getStatus(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
