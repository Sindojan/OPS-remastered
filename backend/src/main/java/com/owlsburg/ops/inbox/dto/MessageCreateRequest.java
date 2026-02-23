package com.owlsburg.ops.inbox.dto;

import com.owlsburg.ops.inbox.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MessageCreateRequest(
        @NotBlank String content,
        @NotNull SenderType senderType,
        UUID senderId
) {}
