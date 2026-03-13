package com.owlsburg.ops.systemagent.dto;

import com.owlsburg.ops.systemagent.SystemApiCredentialEntity;

import java.time.Instant;
import java.util.UUID;

public record ApiCredentialResponse(
        UUID id,
        String serviceName,
        String description,
        boolean hasValue,
        String metadata,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApiCredentialResponse from(SystemApiCredentialEntity entity) {
        return new ApiCredentialResponse(
                entity.getId(),
                entity.getServiceName(),
                entity.getDescription(),
                entity.getEncryptedValue() != null && !entity.getEncryptedValue().isBlank(),
                entity.getMetadata(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
