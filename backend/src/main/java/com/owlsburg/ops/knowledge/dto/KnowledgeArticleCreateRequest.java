package com.owlsburg.ops.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record KnowledgeArticleCreateRequest(
        @NotBlank @Size(max = 255) String title,
        String content,
        @Size(max = 500) String excerpt,
        UUID categoryId,
        List<UUID> tagIds
) {}
