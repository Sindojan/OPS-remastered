package com.sindoflow.ops.customers.dto;

import com.sindoflow.ops.customers.CustomerContactEntity;

import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        UUID customerId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String position,
        boolean isPrimary,
        Instant createdAt,
        Instant updatedAt
) {
    public static ContactResponse from(CustomerContactEntity entity) {
        return new ContactResponse(
                entity.getId(),
                entity.getCustomer().getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getPosition(),
                entity.isPrimary(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
