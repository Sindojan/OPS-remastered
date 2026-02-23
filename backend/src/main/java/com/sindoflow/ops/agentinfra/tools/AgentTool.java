package com.sindoflow.ops.agentinfra.tools;

public interface AgentTool {

    String getName();

    String getDescription();

    String getInputSchema();

    ToolPermission getPermission();

    ToolResult execute(ToolExecutionContext context, String input);
}
