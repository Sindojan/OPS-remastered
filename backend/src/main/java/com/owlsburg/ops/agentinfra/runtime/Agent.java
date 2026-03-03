package com.owlsburg.ops.agentinfra.runtime;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public sealed interface Agent permits CeoAgent, LeadAgent, SubAgent {

    AgentIdentity identity();

    AgentCapabilities capabilities();

    AgentResult execute(AgentContext context, String task);

    default void executeStreaming(AgentContext context, String task, SseEmitter emitter) {
        throw new UnsupportedOperationException(
                "Streaming nicht unterstützt für Agent-Typ: " + identity().role());
    }
}
