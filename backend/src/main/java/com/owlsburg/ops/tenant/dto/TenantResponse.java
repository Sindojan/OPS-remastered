package com.owlsburg.ops.tenant.dto;

import com.owlsburg.ops.tenant.TenantEntity;
import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        String plan,
        String status,
        boolean active,
        Instant createdAt,
        Instant suspendedAt,
        String suspendReason
) {
    public static TenantResponse from(TenantEntity entity) {
        return new TenantResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getPlan(),
                entity.getStatus(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getSuspendedAt(),
                entity.getSuspendReason()
        );
    }
}
