package com.owlsburg.ops.systemagent.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SystemChatRequest(
        @NotBlank String message,
        UUID agentInstanceId,
        UUID sessionId
) {}
