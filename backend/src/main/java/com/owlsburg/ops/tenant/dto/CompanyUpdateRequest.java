package com.owlsburg.ops.tenant.dto;

public record CompanyUpdateRequest(
        String name,
        String plan
) {}
