package com.owlsburg.ops.agentinfra.tools;

public interface AgentTool {

    String getName();

    String getDescription();

    String getInputSchema();

    ToolPermission getPermission();

    ToolResult execute(ToolExecutionContext context, String input);

    /**
     * Returns the module ID this tool belongs to.
     * Tools in deactivated modules won't be available to agents.
     * Default is "core" (always available).
     */
    default String getModuleId() {
        return "core";
    }
}
