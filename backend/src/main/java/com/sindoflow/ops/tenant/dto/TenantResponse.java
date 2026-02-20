package com.sindoflow.ops.tenant.dto;

import com.sindoflow.ops.tenant.TenantEntity;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String tenantId,
        String name,
        boolean active,
        Instant createdAt
) {
    public static TenantResponse from(TenantEntity entity) {
        return new TenantResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
