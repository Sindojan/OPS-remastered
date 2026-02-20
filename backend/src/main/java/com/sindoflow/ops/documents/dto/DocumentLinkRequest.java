package com.sindoflow.ops.documents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DocumentLinkRequest(
        @NotBlank String linkedType,
        @NotNull UUID linkedId
) {}
