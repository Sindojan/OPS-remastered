package com.sindoflow.ops.bom.dto;

import java.util.UUID;

public record UpdatePartRequest(
        String name,
        String description,
        String type,
        UUID unitId,
        String status
) {}
