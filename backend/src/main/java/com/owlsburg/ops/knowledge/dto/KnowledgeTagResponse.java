package com.owlsburg.ops.knowledge.dto;

import com.owlsburg.ops.knowledge.KnowledgeTagEntity;

import java.util.UUID;

public record KnowledgeTagResponse(
        UUID id,
        String name
) {
    public static KnowledgeTagResponse from(KnowledgeTagEntity entity) {
        return new KnowledgeTagResponse(entity.getId(), entity.getName());
    }
}
