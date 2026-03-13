package com.owlsburg.ops.systemagent.runtime;

import com.owlsburg.ops.agentinfra.llm.LlmToolDefinition;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;

import java.util.List;

public record SystemAgentCapabilities(
        List<SystemAgentTool> tools,
        List<LlmToolDefinition> toolDefinitions,
        boolean canDelegate,
        boolean canSpawnSubAgents,
        boolean canCommunicatePeers,
        int maxIterations,
        int maxTokensPerRun,
        Double temperature
) {}
