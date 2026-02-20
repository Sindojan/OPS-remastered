package com.sindoflow.ops.agentinfra.dto;

import com.sindoflow.ops.agentinfra.TriggerType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartAgentRunRequest(
        @NotNull UUID instanceId,
        @NotNull TriggerType triggerType,
        String triggerSource,
        String inputContext
) {}
