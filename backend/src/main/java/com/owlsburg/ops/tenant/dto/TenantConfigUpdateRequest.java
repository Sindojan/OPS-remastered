package com.owlsburg.ops.tenant.dto;

public record TenantConfigUpdateRequest(
        String name,
        String contactEmail,
        String contactPhone,
        String address,
        String city,
        String postalCode,
        String country,
        String website,
        String vatId
) {}
