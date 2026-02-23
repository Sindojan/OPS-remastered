package com.sindoflow.ops.agentinfra.llm;

public record LlmToolDefinition(
        String name,
        String description,
        String inputSchema
) {}
