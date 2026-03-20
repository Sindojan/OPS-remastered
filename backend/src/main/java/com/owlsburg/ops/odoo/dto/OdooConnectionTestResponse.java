package com.owlsburg.ops.odoo.dto;

public record OdooConnectionTestResponse(
        boolean success,
        String serverVersion,
        String message
) {}
