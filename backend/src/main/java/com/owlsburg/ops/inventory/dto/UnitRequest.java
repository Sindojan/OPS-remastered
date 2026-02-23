package com.owlsburg.ops.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record UnitRequest(
        @NotBlank String name,
        @NotBlank String abbreviation
) {}
