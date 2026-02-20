package com.sindoflow.ops.inbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConversationLinkRequest(
        @NotBlank String linkedType,
        @NotNull UUID linkedId
) {}
