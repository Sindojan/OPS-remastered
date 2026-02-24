package com.owlsburg.ops.documents.dto;

import java.util.UUID;

public record DocumentMetadataUpdateRequest(
        String title,
        String description,
        UUID categoryId,
        String excerpt
) {}
