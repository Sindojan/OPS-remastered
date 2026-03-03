package com.owlsburg.ops.agentinfra.runtime;

import java.util.List;

public record AgentResult(
        String output,
        int inputTokens,
        int outputTokens,
        List<String> toolsUsed,
        String status,
        List<LeadStep> steps
) {
    public static AgentResult completed(String output, int inputTokens, int outputTokens, List<String> toolsUsed) {
        return new AgentResult(output, inputTokens, outputTokens, toolsUsed, "completed", List.of());
    }

    public static AgentResult completed(String output, int inputTokens, int outputTokens, List<String> toolsUsed, List<LeadStep> steps) {
        return new AgentResult(output, inputTokens, outputTokens, toolsUsed, "completed", steps);
    }

    public static AgentResult maxIterations(String output, int inputTokens, int outputTokens, List<String> toolsUsed) {
        return new AgentResult(output, inputTokens, outputTokens, toolsUsed, "max_iterations", List.of());
    }

    public static AgentResult error(String errorMessage) {
        return new AgentResult(errorMessage, 0, 0, List.of(), "error", List.of());
    }
}
