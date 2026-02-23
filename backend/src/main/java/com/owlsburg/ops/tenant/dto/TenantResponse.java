package com.owlsburg.ops.tenant.dto;

import com.owlsburg.ops.tenant.TenantEntity;
import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        boolean active,
        Instant createdAt
) {
    public static TenantResponse from(TenantEntity entity) {
        return new TenantResponse(
                entity.getId(),
                entity.getName(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
