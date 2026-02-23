package com.owlsburg.ops.inventory.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryRequest(
        @NotBlank String name,
        UUID parentId
) {}
