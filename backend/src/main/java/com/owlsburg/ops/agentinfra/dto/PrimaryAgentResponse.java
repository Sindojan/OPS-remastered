package com.owlsburg.ops.agentinfra.dto;

import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentTemplateEntity;

import java.util.UUID;

public record PrimaryAgentResponse(UUID id, String name, String role) {

    public static PrimaryAgentResponse from(AgentInstanceEntity instance, AgentTemplateEntity template) {
        return new PrimaryAgentResponse(
                instance.getId(),
                instance.getName(),
                template != null ? template.getRole() : null
        );
    }
}
