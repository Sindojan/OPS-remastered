package com.owlsburg.ops.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CompanyCreateRequest(
        @NotBlank String name,
        @NotBlank String slug,
        String plan,
        @NotBlank @Email String adminEmail,
        @NotBlank String adminFirstName,
        @NotBlank String adminLastName,
        String adminPassword
) {}
