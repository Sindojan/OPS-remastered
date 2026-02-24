package com.owlsburg.ops.knowledge.dto;

import com.owlsburg.ops.knowledge.ArticleStatus;
import com.owlsburg.ops.knowledge.KnowledgeArticleEntity;
import com.owlsburg.ops.knowledge.KnowledgeCategoryEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KnowledgeArticleSummaryResponse(
        UUID id,
        String title,
        String slug,
        String excerpt,
        ArticleStatus status,
        UUID categoryId,
        String categoryName,
        UUID authorId,
        Instant publishedAt,
        List<KnowledgeTagResponse> tags,
        Instant createdAt,
        Instant updatedAt
) {
    public static KnowledgeArticleSummaryResponse from(KnowledgeArticleEntity entity, KnowledgeCategoryEntity category) {
        List<KnowledgeTagResponse> tagResponses = entity.getTags() != null
                ? entity.getTags().stream().map(KnowledgeTagResponse::from).toList()
                : List.of();

        return new KnowledgeArticleSummaryResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getExcerpt(),
                entity.getStatus(),
                entity.getCategoryId(),
                category != null ? category.getName() : null,
                entity.getAuthorId(),
                entity.getPublishedAt(),
                tagResponses,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
