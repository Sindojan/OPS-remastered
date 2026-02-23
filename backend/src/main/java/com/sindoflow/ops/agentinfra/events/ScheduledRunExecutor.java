package com.sindoflow.ops.agentinfra.events;

import com.sindoflow.ops.agentinfra.AgentInstanceEntity;
import com.sindoflow.ops.agentinfra.AgentInstanceService;
import com.sindoflow.ops.agentinfra.AgentInstanceStatus;
import com.sindoflow.ops.agentinfra.TriggerType;
import com.sindoflow.ops.agentinfra.execution.AgentRunOrchestrator;
import com.sindoflow.ops.common.TenantContext;
import com.sindoflow.ops.events.ScheduledTriggerEntity;
import com.sindoflow.ops.events.ScheduledTriggerService;
import com.sindoflow.ops.tenant.TenantEntity;
import com.sindoflow.ops.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduledRunExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRunExecutor.class);

    private final TenantRepository tenantRepository;
    private final ScheduledTriggerService scheduledTriggerService;
    private final AgentInstanceService agentInstanceService;
    private final AgentRunOrchestrator agentRunOrchestrator;

    public ScheduledRunExecutor(TenantRepository tenantRepository,
                                 ScheduledTriggerService scheduledTriggerService,
                                 AgentInstanceService agentInstanceService,
                                 AgentRunOrchestrator agentRunOrchestrator) {
        this.tenantRepository = tenantRepository;
        this.scheduledTriggerService = scheduledTriggerService;
        this.agentInstanceService = agentInstanceService;
        this.agentRunOrchestrator = agentRunOrchestrator;
    }

    @Scheduled(fixedDelay = 60000)
    public void executeScheduledRuns() {
        List<TenantEntity> tenants = tenantRepository.findByActiveTrue();

        for (TenantEntity tenant : tenants) {
            try {
                TenantContext.setCurrentTenant(tenant.getTenantId());
                executeTriggersForTenant(tenant.getTenantId());
            } catch (Exception e) {
                log.error("Error executing scheduled runs for tenant {}: {}", tenant.getTenantId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void executeTriggersForTenant(String schemaName) {
        List<ScheduledTriggerEntity> triggers = scheduledTriggerService.getTriggersToRun();

        if (triggers.isEmpty()) {
            return;
        }

        log.info("Executing {} scheduled triggers for tenant {}", triggers.size(), schemaName);

        for (ScheduledTriggerEntity trigger : triggers) {
            try {
                AgentInstanceEntity instance = agentInstanceService.findById(trigger.getInstanceId());

                if (instance.getStatus() != AgentInstanceStatus.ACTIVE) {
                    log.debug("Skipping trigger {} - instance {} is not ACTIVE (status={})",
                            trigger.getId(), instance.getId(), instance.getStatus());
                    scheduledTriggerService.updateAfterRun(trigger.getId());
                    continue;
                }

                log.info("Triggering scheduled run for instance {} (cron={}, triggerId={})",
                        instance.getName(), trigger.getCronExpression(), trigger.getId());

                agentRunOrchestrator.triggerRun(
                        instance.getId(),
                        TriggerType.SCHEDULE,
                        "cron:" + trigger.getCronExpression(),
                        "{\"triggerId\":\"" + trigger.getId() + "\"}"
                );

                scheduledTriggerService.updateAfterRun(trigger.getId());
            } catch (Exception e) {
                log.error("Error executing trigger {}: {}", trigger.getId(), e.getMessage(), e);
            }
        }
    }
}
