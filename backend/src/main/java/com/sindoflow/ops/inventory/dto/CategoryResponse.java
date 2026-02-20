package com.sindoflow.ops.inventory.dto;

import com.sindoflow.ops.inventory.ArticleCategoryEntity;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        UUID parentId,
        Instant createdAt
) {
    public static CategoryResponse from(ArticleCategoryEntity e) {
        return new CategoryResponse(e.getId(), e.getName(), e.getParentId(), e.getCreatedAt());
    }
}
