package com.owlsburg.ops.agentinfra.llm;

public record LlmToolDefinition(
        String name,
        String description,
        String inputSchema
) {}
