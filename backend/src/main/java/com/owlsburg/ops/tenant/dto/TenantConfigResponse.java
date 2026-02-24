package com.owlsburg.ops.tenant.dto;

import com.owlsburg.ops.tenant.TenantEntity;

import java.util.UUID;

public record TenantConfigResponse(
        UUID id,
        String name,
        String slug,
        String plan,
        String status,
        String logoUrl,
        String contactEmail,
        String contactPhone,
        String address,
        String city,
        String postalCode,
        String country,
        String website,
        String vatId
) {
    public static TenantConfigResponse from(TenantEntity entity) {
        return new TenantConfigResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getPlan(),
                entity.getStatus(),
                entity.getLogoUrl(),
                entity.getContactEmail(),
                entity.getContactPhone(),
                entity.getAddress(),
                entity.getCity(),
                entity.getPostalCode(),
                entity.getCountry(),
                entity.getWebsite(),
                entity.getVatId()
        );
    }
}
