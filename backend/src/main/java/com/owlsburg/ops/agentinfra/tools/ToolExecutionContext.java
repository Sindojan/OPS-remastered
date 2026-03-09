package com.owlsburg.ops.agentinfra.tools;

import com.owlsburg.ops.agentinfra.runtime.AgentActivityBus;
import com.owlsburg.ops.agentinfra.runtime.RunMemory;

import java.util.UUID;

public record ToolExecutionContext(String tenantId, UUID instanceId, UUID runId, AgentActivityBus activityBus, RunMemory runMemory) {
    public ToolExecutionContext(String tenantId, UUID instanceId, UUID runId) {
        this(tenantId, instanceId, runId, null, null);
    }

    public ToolExecutionContext(String tenantId, UUID instanceId, UUID runId, AgentActivityBus activityBus) {
        this(tenantId, instanceId, runId, activityBus, null);
    }
}
