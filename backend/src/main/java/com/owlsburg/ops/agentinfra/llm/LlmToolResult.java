package com.owlsburg.ops.agentinfra.llm;

public record LlmToolResult(
        String toolUseId,
        String content
) {}
