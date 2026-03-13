package com.owlsburg.ops.systemagent.runtime;

import com.owlsburg.ops.agentinfra.runtime.RunMemory;

import java.time.Instant;
import java.util.UUID;

public record SystemAgentContext(
        UUID invocationId,
        UUID userId,
        UUID chatSessionId,
        int delegationDepth,
        Instant startedAt,
        SystemAgentActivityBus activityBus,
        RunMemory runMemory
) {
    public static SystemAgentContext forChat(UUID userId, UUID chatSessionId) {
        return new SystemAgentContext(UUID.randomUUID(), userId, chatSessionId, 0, Instant.now(), null, new RunMemory());
    }

    public static SystemAgentContext forChat(UUID userId, UUID chatSessionId, SystemAgentActivityBus activityBus) {
        return new SystemAgentContext(UUID.randomUUID(), userId, chatSessionId, 0, Instant.now(), activityBus, new RunMemory());
    }

    public SystemAgentContext withDelegationDepth(int depth) {
        return new SystemAgentContext(invocationId, userId, chatSessionId, depth, startedAt, activityBus, runMemory);
    }
}
