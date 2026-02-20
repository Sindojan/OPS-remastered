package com.sindoflow.ops.agentinfra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateAgentTemplateRequest(
        @NotBlank String name,
        @NotBlank String role,
        String description,
        String basePrompt,
        String allowedTools,
        String triggerTypes,
        @Positive int maxTokensPerRun,
        @Positive int dailyTokenBudget,
        String status,
        int version
) {}
