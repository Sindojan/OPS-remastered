package com.sindoflow.ops.agentinfra.dto;

import jakarta.validation.constraints.NotBlank;

public record FailAgentRunRequest(
        @NotBlank String errorMessage
) {}
