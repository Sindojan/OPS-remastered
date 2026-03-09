package com.owlsburg.ops.common.dto;

public record ModuleResponse(
        String id,
        String label,
        String description,
        boolean core,
        int displayOrder,
        boolean enabled
) {}
