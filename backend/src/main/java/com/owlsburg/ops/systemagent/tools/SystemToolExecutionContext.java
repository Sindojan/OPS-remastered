package com.owlsburg.ops.systemagent.tools;

import com.owlsburg.ops.agentinfra.runtime.RunMemory;
import com.owlsburg.ops.systemagent.runtime.SystemAgentActivityBus;

import java.util.UUID;

public record SystemToolExecutionContext(
        UUID instanceId,
        UUID runId,
        SystemAgentActivityBus activityBus,
        RunMemory runMemory
) {
    public SystemToolExecutionContext(UUID instanceId, UUID runId) {
        this(instanceId, runId, null, null);
    }
}
