package com.owlsburg.ops.systemagent.runtime;

import com.owlsburg.ops.agentinfra.AgentInstanceType;

import java.util.List;
import java.util.UUID;

public record SystemAgentIdentity(
        UUID instanceId,
        UUID templateId,
        String name,
        String role,
        AgentInstanceType type,
        UUID parentInstanceId,
        String systemPrompt,
        String model,
        List<String> allowedToolNames
) {}
