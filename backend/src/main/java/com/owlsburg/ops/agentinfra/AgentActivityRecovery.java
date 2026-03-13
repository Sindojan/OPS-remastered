package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.systemagent.SystemAgentInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resets stale BUSY agent instances to IDLE on application startup.
 * This handles the case where agents were mid-execution when the server
 * was stopped or crashed, leaving them permanently stuck in BUSY state.
 */
@Component
public class AgentActivityRecovery {

    private static final Logger log = LoggerFactory.getLogger(AgentActivityRecovery.class);

    private final AgentInstanceRepository agentInstanceRepository;
    private final SystemAgentInstanceRepository systemAgentInstanceRepository;

    public AgentActivityRecovery(AgentInstanceRepository agentInstanceRepository,
                                 SystemAgentInstanceRepository systemAgentInstanceRepository) {
        this.agentInstanceRepository = agentInstanceRepository;
        this.systemAgentInstanceRepository = systemAgentInstanceRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void resetStaleAgents() {
        int tenantAgents = agentInstanceRepository.resetAllBusyToIdle();
        int systemAgents = systemAgentInstanceRepository.resetAllBusyToIdle();

        if (tenantAgents > 0 || systemAgents > 0) {
            log.info("Agent activity recovery: reset {} tenant agents and {} system agents from BUSY to IDLE",
                    tenantAgents, systemAgents);
        }
    }
}
