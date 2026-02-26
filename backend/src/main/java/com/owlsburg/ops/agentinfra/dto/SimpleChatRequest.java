package com.owlsburg.ops.agentinfra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SimpleChatRequest(
        @NotBlank String message,
        @NotNull UUID agentInstanceId,
        List<ChatHistoryMessage> history
) {
    public record ChatHistoryMessage(String role, String content) {}
}
