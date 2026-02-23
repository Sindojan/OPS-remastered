package com.sindoflow.ops.agentinfra.llm;

public record LlmResponse(
        String content,
        LlmToolUse toolUse,
        String stopReason,
        int inputTokens,
        int outputTokens,
        String model
) {}
