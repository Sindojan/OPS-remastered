package com.owlsburg.ops.agentinfra.dto;

public record AvailableToolResponse(
        String name,
        String description,
        String permissionLevel
) {}
