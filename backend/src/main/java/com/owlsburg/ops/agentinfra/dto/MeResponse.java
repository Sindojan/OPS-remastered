package com.owlsburg.ops.agentinfra.dto;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        UUID tenantId,
        PrimaryAgentResponse primaryAgentInstance
) {}
