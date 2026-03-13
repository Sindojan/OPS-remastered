package com.owlsburg.ops.systemagent.runtime;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public sealed interface SystemAgent permits SystemCeoAgent, SystemLeadAgent, SystemSubAgent {

    SystemAgentIdentity identity();

    SystemAgentCapabilities capabilities();

    SystemAgentResult execute(SystemAgentContext context, String task);

    default void executeStreaming(SystemAgentContext context, String task, SseEmitter emitter) {
        throw new UnsupportedOperationException(
                "Streaming nicht unterstützt für System-Agent-Typ: " + identity().role());
    }
}
