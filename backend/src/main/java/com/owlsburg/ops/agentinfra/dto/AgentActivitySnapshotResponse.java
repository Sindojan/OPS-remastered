package com.owlsburg.ops.agentinfra.dto;

import java.util.List;

public record AgentActivitySnapshotResponse(
        List<AgentInstanceActivity> instances,
        List<ActiveLinkDto> activeLinks
) {}
