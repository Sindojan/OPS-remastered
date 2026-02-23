package com.owlsburg.ops.agentinfra.llm;

public record LlmConfigResponse(
        String provider,
        String defaultModel,
        boolean hasApiKey
) {}
