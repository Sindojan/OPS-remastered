package com.sindoflow.ops.customers.dto;

public record CustomerUpdateRequest(
        String companyName,
        String taxId
) {}
