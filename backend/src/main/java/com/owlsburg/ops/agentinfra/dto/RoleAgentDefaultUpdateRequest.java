package com.owlsburg.ops.agentinfra.dto;

import java.util.UUID;

public record RoleAgentDefaultUpdateRequest(String role, UUID agentInstanceId) {}
