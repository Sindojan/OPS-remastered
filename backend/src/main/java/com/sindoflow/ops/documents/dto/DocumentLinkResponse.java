package com.sindoflow.ops.documents.dto;

import com.sindoflow.ops.documents.DocumentLinkEntity;

import java.time.Instant;
import java.util.UUID;

public record DocumentLinkResponse(
        UUID id,
        UUID documentId,
        String linkedType,
        UUID linkedId,
        Instant createdAt
) {
    public static DocumentLinkResponse from(DocumentLinkEntity entity) {
        return new DocumentLinkResponse(
                entity.getId(),
                entity.getDocumentId(),
                entity.getLinkedType(),
                entity.getLinkedId(),
                entity.getCreatedAt()
        );
    }
}
