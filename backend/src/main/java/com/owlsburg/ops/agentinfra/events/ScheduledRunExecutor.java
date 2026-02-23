package com.owlsburg.ops.agentinfra.events;

import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentInstanceService;
import com.owlsburg.ops.agentinfra.AgentInstanceStatus;
import com.owlsburg.ops.agentinfra.TriggerType;
import com.owlsburg.ops.agentinfra.execution.AgentRunOrchestrator;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.events.ScheduledTriggerEntity;
import com.owlsburg.ops.events.ScheduledTriggerService;
import com.owlsburg.ops.tenant.TenantEntity;
import com.owlsburg.ops.tenant.TenantRepository;
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
                TenantContext.setCurrentTenant(tenant.getId().toString());
                executeTriggersForTenant(tenant.getId().toString());
            } catch (Exception e) {
                log.error("Error executing scheduled runs for tenant {}: {}", tenant.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void executeTriggersForTenant(String tenantId) {
        List<ScheduledTriggerEntity> triggers = scheduledTriggerService.getTriggersToRun();

        if (triggers.isEmpty()) {
            return;
        }

        log.info("Executing {} scheduled triggers for tenant {}", triggers.size(), tenantId);

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
