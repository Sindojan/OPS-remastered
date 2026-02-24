package com.owlsburg.ops.knowledge.dto;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeSearchResultResponse(
        String type,
        UUID id,
        String title,
        String excerpt,
        String category,
        Instant updatedAt
) {}
