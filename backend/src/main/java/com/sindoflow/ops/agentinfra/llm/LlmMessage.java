package com.sindoflow.ops.agentinfra.llm;

public record LlmMessage(
        String role,
        String content,
        LlmToolUse toolUse,
        LlmToolResult toolResult
) {

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content, null, null);
    }

    public static LlmMessage assistantToolUse(LlmToolUse toolUse) {
        return new LlmMessage("assistant", null, toolUse, null);
    }

    public static LlmMessage toolResult(LlmToolResult result) {
        return new LlmMessage("user", null, null, result);
    }
}
