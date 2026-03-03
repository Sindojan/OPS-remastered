package com.owlsburg.ops.agentinfra.runtime;

import java.util.List;

public record AgentResult(
        String output,
        int inputTokens,
        int outputTokens,
        List<String> toolsUsed,
        String status
) {
    public static AgentResult completed(String output, int inputTokens, int outputTokens, List<String> toolsUsed) {
        return new AgentResult(output, inputTokens, outputTokens, toolsUsed, "completed");
    }

    public static AgentResult maxIterations(String output, int inputTokens, int outputTokens, List<String> toolsUsed) {
        return new AgentResult(output, inputTokens, outputTokens, toolsUsed, "max_iterations");
    }

    public static AgentResult error(String errorMessage) {
        return new AgentResult(errorMessage, 0, 0, List.of(), "error");
    }
}
