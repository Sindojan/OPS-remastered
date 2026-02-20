package com.sindoflow.ops.documents.dto;

import com.sindoflow.ops.documents.DocumentEntity;
import com.sindoflow.ops.documents.DocumentStatus;

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
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
