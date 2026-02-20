package com.sindoflow.ops.customers.dto;

import com.sindoflow.ops.customers.CustomerEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String companyName,
        String taxId,
        String status,
        List<ContactResponse> contacts,
        List<AddressResponse> addresses,
        List<PriceGroupResponse> priceGroups,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(CustomerEntity entity) {
        List<ContactResponse> contacts = entity.getContacts() != null
                ? entity.getContacts().stream().map(ContactResponse::from).toList()
                : List.of();

        List<AddressResponse> addresses = entity.getAddresses() != null
                ? entity.getAddresses().stream().map(AddressResponse::from).toList()
                : List.of();

        List<PriceGroupResponse> priceGroups = entity.getPriceGroups() != null
                ? entity.getPriceGroups().stream().map(PriceGroupResponse::from).toList()
                : List.of();

        return new CustomerResponse(
                entity.getId(),
                entity.getCompanyName(),
                entity.getTaxId(),
                entity.getStatus(),
                contacts,
                addresses,
                priceGroups,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static CustomerResponse fromSummary(CustomerEntity entity) {
        return new CustomerResponse(
                entity.getId(),
                entity.getCompanyName(),
                entity.getTaxId(),
                entity.getStatus(),
                List.of(),
                List.of(),
                List.of(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
