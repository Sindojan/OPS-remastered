package com.sindoflow.ops.customers.dto;

import com.sindoflow.ops.customers.AddressType;
import com.sindoflow.ops.customers.CustomerAddressEntity;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        UUID customerId,
        AddressType type,
        String street,
        String zip,
        String city,
        String country,
        Instant createdAt,
        Instant updatedAt
) {
    public static AddressResponse from(CustomerAddressEntity entity) {
        return new AddressResponse(
                entity.getId(),
                entity.getCustomer().getId(),
                entity.getType(),
                entity.getStreet(),
                entity.getZip(),
                entity.getCity(),
                entity.getCountry(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
