package com.owlsburg.ops.inventory.dto;

import com.owlsburg.ops.inventory.UnitEntity;

import java.util.UUID;

public record UnitResponse(
        UUID id,
        String name,
        String abbreviation
) {
    public static UnitResponse from(UnitEntity e) {
        return new UnitResponse(e.getId(), e.getName(), e.getAbbreviation());
    }
}
