package com.owlsburg.ops.documents.dto;

import com.owlsburg.ops.documents.DocumentEntity;
import com.owlsburg.ops.documents.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String description,
        String category,
        String fileKey,
        String fileName,
        String mimeType,
        Long fileSizeBytes,
        int version,
        DocumentStatus status,
        UUID uploadedBy,
        UUID categoryId,
        String excerpt,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(DocumentEntity entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getFileKey(),
                entity.getFileName(),
                entity.getMimeType(),
                entity.getFileSizeBytes(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getUploadedBy(),
                entity.getCategoryId(),
                entity.getExcerpt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
