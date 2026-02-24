package com.owlsburg.ops.tenant.dto;

import java.util.UUID;

public record CompanyCreateResponse(
        UUID id,
        String name,
        String slug,
        String plan,
        String adminEmail,
        String adminPassword
) {}
