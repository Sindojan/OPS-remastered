package com.owlsburg.ops.systemagent.events;

import com.owlsburg.ops.agentinfra.AgentInstanceStatus;
import com.owlsburg.ops.agentinfra.TriggerType;
import com.owlsburg.ops.agentinfra.runtime.RunMemory;
import com.owlsburg.ops.systemagent.*;
import com.owlsburg.ops.systemagent.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class SystemScheduledRunExecutor {

    private static final Logger log = LoggerFactory.getLogger(SystemScheduledRunExecutor.class);

    private final SystemScheduledTriggerRepository triggerRepository;
    private final SystemAgentInstanceRepository instanceRepository;
    private final SystemAgentRunService runService;
    private final SystemAgentFactory agentFactory;
    private final SystemAgentInstanceService instanceService;

    public SystemScheduledRunExecutor(SystemScheduledTriggerRepository triggerRepository,
                                       SystemAgentInstanceRepository instanceRepository,
                                       SystemAgentRunService runService,
                                       SystemAgentFactory agentFactory,
                                       SystemAgentInstanceService instanceService) {
        this.triggerRepository = triggerRepository;
        this.instanceRepository = instanceRepository;
        this.runService = runService;
        this.agentFactory = agentFactory;
        this.instanceService = instanceService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void executeDueTriggers() {
        List<SystemScheduledTriggerEntity> enabledTriggers;
        try {
            enabledTriggers = triggerRepository.findByEnabledTrue();
        } catch (Exception e) {
            log.debug("Scheduled trigger table not yet available: {}", e.getMessage());
            return;
        }

        if (enabledTriggers.isEmpty()) {
            return;
        }

        int executed = 0;
        int skipped = 0;
        int failed = 0;

        for (SystemScheduledTriggerEntity trigger : enabledTriggers) {
            try {
                if (!isDue(trigger)) {
                    skipped++;
                    continue;
                }

                // Load instance, skip if not ACTIVE
                SystemAgentInstanceEntity instance = instanceRepository.findById(trigger.getInstanceId())
                        .orElse(null);

                if (instance == null || instance.getStatus() != AgentInstanceStatus.ACTIVE) {
                    log.debug("Skipping trigger {} – instance {} not active",
                            trigger.getId(), trigger.getInstanceId());
                    skipped++;
                    continue;
                }

                // Create run
                SystemAgentRunEntity run = runService.startRun(
                        trigger.getInstanceId(),
                        TriggerType.SCHEDULE,
                        "Scheduled: " + trigger.getCronExpression(),
                        null);

                try {
                    // Create and execute agent
                    SystemAgent agent = agentFactory.createAgent(trigger.getInstanceId());
                    SystemAgentContext agentContext = new SystemAgentContext(
                            run.getId(), null, null, 0, Instant.now(), null, new RunMemory());

                    SystemAgentResult result = agent.execute(agentContext, trigger.getTaskDescription());

                    // Complete run
                    int totalTokens = result.inputTokens() + result.outputTokens();
                    runService.completeRun(run.getId(), result.output(), totalTokens, null);
                    instanceService.linkLastRun(trigger.getInstanceId(), run.getId());

                    executed++;
                } catch (Exception e) {
                    log.error("Error executing scheduled system agent run {} for trigger {}: {}",
                            run.getId(), trigger.getId(), e.getMessage());
                    runService.failRun(run.getId(), e.getMessage());
                    failed++;
                }

                // Update lastRunAt
                trigger.setLastRunAt(Instant.now());
                triggerRepository.save(trigger);

            } catch (Exception e) {
                log.error("Error processing trigger {}: {}", trigger.getId(), e.getMessage());
                failed++;
            }
        }

        if (executed > 0 || failed > 0) {
            log.info("System scheduled run executor: {} executed, {} skipped, {} failed (of {} triggers)",
                    executed, skipped, failed, enabledTriggers.size());
        }
    }

    private boolean isDue(SystemScheduledTriggerEntity trigger) {
        try {
            CronExpression cron = CronExpression.parse(trigger.getCronExpression());

            LocalDateTime reference;
            if (trigger.getLastRunAt() != null) {
                reference = LocalDateTime.ofInstant(trigger.getLastRunAt(), ZoneOffset.UTC);
            } else {
                // Never run before – use a reference point far enough back
                reference = LocalDateTime.ofInstant(Instant.now().minusSeconds(86400), ZoneOffset.UTC);
            }

            LocalDateTime next = cron.next(reference);
            if (next == null) {
                return false;
            }

            Instant nextInstant = next.toInstant(ZoneOffset.UTC);
            return nextInstant.isBefore(Instant.now());
        } catch (Exception e) {
            log.warn("Invalid cron expression '{}' for trigger {}: {}",
                    trigger.getCronExpression(), trigger.getId(), e.getMessage());
            return false;
        }
    }
}
