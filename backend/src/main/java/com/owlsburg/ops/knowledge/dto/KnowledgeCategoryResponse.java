package com.owlsburg.ops.knowledge.dto;

import com.owlsburg.ops.knowledge.KnowledgeCategoryEntity;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeCategoryResponse(
        UUID id,
        String name,
        String color,
        Instant createdAt
) {
    public static KnowledgeCategoryResponse from(KnowledgeCategoryEntity entity) {
        return new KnowledgeCategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getColor(),
                entity.getCreatedAt()
        );
    }
}
