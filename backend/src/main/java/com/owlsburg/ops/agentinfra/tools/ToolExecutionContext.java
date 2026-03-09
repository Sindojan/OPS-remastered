package com.owlsburg.ops.agentinfra.tools;

import com.owlsburg.ops.agentinfra.runtime.AgentActivityBus;

import java.util.UUID;

public record ToolExecutionContext(String tenantId, UUID instanceId, UUID runId, AgentActivityBus activityBus) {
    public ToolExecutionContext(String tenantId, UUID instanceId, UUID runId) {
        this(tenantId, instanceId, runId, null);
    }
}
