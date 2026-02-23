package com.owlsburg.ops.agentinfra.execution;

import com.owlsburg.ops.agentinfra.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AgentRunOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentRunOrchestrator.class);

    private final AgentRunService runService;
    private final AgentExecutionService executionService;
    private final AgentIncidentService incidentService;

    public AgentRunOrchestrator(AgentRunService runService,
                                 AgentExecutionService executionService,
                                 AgentIncidentService incidentService) {
        this.runService = runService;
        this.executionService = executionService;
        this.incidentService = incidentService;
    }

    @Async("agentExecutor")
    public void triggerRun(UUID instanceId, TriggerType triggerType, String triggerSource, String inputContext) {
        AgentRunEntity run = runService.startRun(instanceId, triggerType, triggerSource, inputContext);
        log.info("Triggered async agent run {} for instance {}", run.getId(), instanceId);

        try {
            executionService.executeRun(run.getId(), 0);
        } catch (Exception e) {
            log.error("Agent run {} failed with unexpected error: {}", run.getId(), e.getMessage(), e);
            try {
                runService.failRun(run.getId(), "Unerwarteter Fehler: " + e.getMessage());
            } catch (Exception failEx) {
                log.error("Failed to mark run {} as failed: {}", run.getId(), failEx.getMessage());
            }

            // Create incident
            try {
                AgentRunEntity failedRun = runService.findById(run.getId());
                incidentService.report(
                        failedRun.getInstanceId(),
                        "EXECUTION_ERROR",
                        "Run " + run.getId() + " fehlgeschlagen: " + e.getMessage()
                );
            } catch (Exception incidentEx) {
                log.error("Failed to create incident for run {}: {}", run.getId(), incidentEx.getMessage());
            }
        }
    }
}
