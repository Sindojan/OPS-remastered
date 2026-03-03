package com.owlsburg.ops.agentinfra.runtime;

import java.time.Instant;
import java.util.UUID;

public record AgentContext(
        UUID invocationId,
        String tenantId,
        UUID userId,
        UUID chatSessionId,
        int delegationDepth,
        Instant startedAt
) {
    public static AgentContext forChat(String tenantId, UUID userId, UUID chatSessionId) {
        return new AgentContext(UUID.randomUUID(), tenantId, userId, chatSessionId, 0, Instant.now());
    }

    public AgentContext withDelegationDepth(int depth) {
        return new AgentContext(invocationId, tenantId, userId, chatSessionId, depth, startedAt);
    }
}
