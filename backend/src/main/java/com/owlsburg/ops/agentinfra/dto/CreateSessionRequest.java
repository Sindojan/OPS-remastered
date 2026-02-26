package com.owlsburg.ops.agentinfra.dto;

import java.util.UUID;

public record CreateSessionRequest(UUID agentInstanceId) {
}
