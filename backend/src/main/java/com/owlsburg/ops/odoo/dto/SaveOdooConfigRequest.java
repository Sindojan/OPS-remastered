package com.owlsburg.ops.odoo.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveOdooConfigRequest(
        @NotBlank String baseUrl,
        @NotBlank String databaseName,
        @NotBlank String apiKey,
        String odooVersion
) {}
