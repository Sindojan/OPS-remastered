package com.owlsburg.ops.agentinfra.runtime;

import com.owlsburg.ops.agentinfra.AgentInstanceType;

import java.util.List;
import java.util.UUID;

public record AgentIdentity(
        UUID instanceId,
        UUID templateId,
        UUID tenantId,
        String name,
        String role,
        AgentInstanceType type,
        UUID parentInstanceId,
        String systemPrompt,
        String model,
        List<String> allowedToolNames
) {}
