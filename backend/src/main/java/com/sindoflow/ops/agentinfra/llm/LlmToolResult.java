package com.sindoflow.ops.agentinfra.llm;

public record LlmToolResult(
        String toolUseId,
        String content
) {}
