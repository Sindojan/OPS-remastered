package com.sindoflow.ops.production.dto;

public record UpdateStationRequest(
        String name,
        String description,
        Integer capacityPerShift,
        String status
) {}
