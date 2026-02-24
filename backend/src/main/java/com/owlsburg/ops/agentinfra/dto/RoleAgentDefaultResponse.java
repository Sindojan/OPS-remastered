package com.owlsburg.ops.agentinfra.dto;

import java.util.UUID;

public record RoleAgentDefaultResponse(UUID id, String role, UUID agentInstanceId, String agentInstanceName) {}
