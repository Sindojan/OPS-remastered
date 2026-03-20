package com.owlsburg.ops.odoo.dto;

import java.time.Instant;

public record OdooConfigResponse(
        String baseUrl,
        String databaseName,
        String odooVersion,
        boolean hasApiKey,
        String connectionStatus,
        Instant lastConnectedAt
) {}
