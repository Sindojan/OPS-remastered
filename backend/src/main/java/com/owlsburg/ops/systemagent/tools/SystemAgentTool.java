package com.owlsburg.ops.systemagent.tools;

public interface SystemAgentTool {

    String getName();

    String getDescription();

    String getInputSchema();

    SystemToolResult execute(SystemToolExecutionContext context, String input);
}
