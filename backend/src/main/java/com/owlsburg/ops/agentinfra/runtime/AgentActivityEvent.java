package com.owlsburg.ops.agentinfra.runtime;

import java.time.Instant;
import java.util.UUID;

public record AgentActivityEvent(
        Type type,
        UUID agentInstanceId,
        UUID targetInstanceId,
        String agentName,
        String detail,
        Instant timestamp
) {
    public enum Type {
        THINKING,
        TOOL_CALL,
        TOOL_RESULT,
        DELEGATION_START,
        DELEGATION_END,
        IDLE
    }
}
