package com.owlsburg.ops.agentinfra.runtime;

public record LeadStep(
        String type,
        String toolName,
        String content,
        int iteration
) {
}
