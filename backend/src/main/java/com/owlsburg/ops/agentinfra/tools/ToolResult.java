package com.owlsburg.ops.agentinfra.tools;

public record ToolResult(boolean success, String data, String errorMessage) {

    public static ToolResult success(String data) {
        return new ToolResult(true, data, null);
    }

    public static ToolResult error(String message) {
        return new ToolResult(false, null, message);
    }
}
