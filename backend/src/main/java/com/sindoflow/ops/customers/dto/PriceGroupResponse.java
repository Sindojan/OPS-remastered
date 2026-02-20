package com.sindoflow.ops.customers.dto;

import com.sindoflow.ops.customers.CustomerPriceGroupEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PriceGroupResponse(
        UUID id,
        UUID customerId,
        String name,
        BigDecimal discountPercent,
        LocalDate validFrom,
        LocalDate validUntil,
        Instant createdAt,
        Instant updatedAt
) {
    public static PriceGroupResponse from(CustomerPriceGroupEntity entity) {
        return new PriceGroupResponse(
                entity.getId(),
                entity.getCustomer().getId(),
                entity.getName(),
                entity.getDiscountPercent(),
                entity.getValidFrom(),
                entity.getValidUntil(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
