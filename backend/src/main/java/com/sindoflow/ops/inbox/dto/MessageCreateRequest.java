package com.sindoflow.ops.inbox.dto;

import com.sindoflow.ops.inbox.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MessageCreateRequest(
        @NotBlank String content,
        @NotNull SenderType senderType,
        UUID senderId
) {}
