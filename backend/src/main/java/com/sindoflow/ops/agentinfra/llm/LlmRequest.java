package com.sindoflow.ops.agentinfra.llm;

import java.util.List;

public record LlmRequest(
        String systemPrompt,
        List<LlmMessage> messages,
        List<LlmToolDefinition> tools,
        String model,
        int maxTokens
) {}
