package com.owlsburg.ops.customers.dto;

public record CustomerUpdateRequest(
        String companyName,
        String taxId
) {}
