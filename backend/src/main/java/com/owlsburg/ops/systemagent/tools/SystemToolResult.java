package com.owlsburg.ops.systemagent.tools;

public record SystemToolResult(boolean success, String data, String errorMessage) {

    public static SystemToolResult success(String data) {
        return new SystemToolResult(true, data, null);
    }

    public static SystemToolResult error(String message) {
        return new SystemToolResult(false, null, message);
    }
}
