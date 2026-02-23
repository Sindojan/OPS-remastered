package com.owlsburg.ops.agentinfra.tools;

import java.util.UUID;

public record ToolExecutionContext(String tenantId, UUID instanceId, UUID runId) {
}
