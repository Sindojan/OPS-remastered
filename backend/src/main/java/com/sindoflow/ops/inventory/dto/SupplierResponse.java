package com.sindoflow.ops.inventory.dto;

import com.sindoflow.ops.inventory.SupplierEntity;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String contactName,
        String email,
        String phone,
        String address,
        String taxId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupplierResponse from(SupplierEntity e) {
        return new SupplierResponse(
                e.getId(), e.getName(), e.getContactName(), e.getEmail(),
                e.getPhone(), e.getAddress(), e.getTaxId(), e.getStatus(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
