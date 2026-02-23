package com.owlsburg.ops.agentinfra.llm;

import jakarta.validation.constraints.NotBlank;

public record SaveLlmConfigRequest(
        String provider,
        @NotBlank String apiKey,
        String defaultModel
) {}
