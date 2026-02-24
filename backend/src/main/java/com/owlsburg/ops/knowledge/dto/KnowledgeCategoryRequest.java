package com.owlsburg.ops.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 7) String color
) {}
