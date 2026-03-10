package com.owlsburg.ops.agentinfra.runtime;

import com.owlsburg.ops.agentinfra.llm.LlmToolDefinition;
import com.owlsburg.ops.agentinfra.tools.AgentTool;

import java.util.List;

public record AgentCapabilities(
        List<AgentTool> tools,
        List<LlmToolDefinition> toolDefinitions,
        boolean canDelegate,
        boolean canSpawnSubAgents,
        boolean canCommunicatePeers,
        int maxIterations,
        int maxTokensPerRun,
        Double temperature
) {}
