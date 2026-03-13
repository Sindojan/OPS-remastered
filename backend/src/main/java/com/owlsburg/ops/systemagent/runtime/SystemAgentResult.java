package com.owlsburg.ops.systemagent.runtime;

import com.owlsburg.ops.agentinfra.runtime.LeadStep;

import java.util.List;

public record SystemAgentResult(
        String output,
        int inputTokens,
        int outputTokens,
        List<String> toolsUsed,
        String status,
        List<LeadStep> steps
) {
    public static SystemAgentResult completed(String output, int inputTokens, int outputTokens, List<String> toolsUsed) {
        return new SystemAgentResult(output, inputTokens, outputTokens, toolsUsed, "completed", List.of());
    }

    public static SystemAgentResult completed(String output, int inputTokens, int outputTokens, List<String> toolsUsed, List<LeadStep> steps) {
        return new SystemAgentResult(output, inputTokens, outputTokens, toolsUsed, "completed", steps);
    }

    public static SystemAgentResult maxIterations(String output, int inputTokens, int outputTokens, List<String> toolsUsed) {
        return new SystemAgentResult(output, inputTokens, outputTokens, toolsUsed, "max_iterations", List.of());
    }

    public static SystemAgentResult error(String errorMessage) {
        return new SystemAgentResult(errorMessage, 0, 0, List.of(), "error", List.of());
    }
}
