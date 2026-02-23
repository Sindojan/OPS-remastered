package com.owlsburg.ops.agentinfra.events;

import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentInstanceService;
import com.owlsburg.ops.agentinfra.AgentInstanceStatus;
import com.owlsburg.ops.agentinfra.TriggerType;
import com.owlsburg.ops.agentinfra.execution.AgentRunOrchestrator;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.events.DomainEventEntity;
import com.owlsburg.ops.events.DomainEventService;
import com.owlsburg.ops.tenant.TenantEntity;
import com.owlsburg.ops.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DomainEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(DomainEventProcessor.class);

    private final TenantRepository tenantRepository;
    private final DomainEventService domainEventService;
    private final AgentEventSubscriptionRepository subscriptionRepository;
    private final AgentInstanceService agentInstanceService;
    private final AgentRunOrchestrator agentRunOrchestrator;

    public DomainEventProcessor(TenantRepository tenantRepository,
                                 DomainEventService domainEventService,
                                 AgentEventSubscriptionRepository subscriptionRepository,
                                 AgentInstanceService agentInstanceService,
                                 AgentRunOrchestrator agentRunOrchestrator) {
        this.tenantRepository = tenantRepository;
        this.domainEventService = domainEventService;
        this.subscriptionRepository = subscriptionRepository;
        this.agentInstanceService = agentInstanceService;
        this.agentRunOrchestrator = agentRunOrchestrator;
    }

    @Scheduled(fixedDelay = 10000)
    public void processEvents() {
        List<TenantEntity> tenants = tenantRepository.findByActiveTrue();

        for (TenantEntity tenant : tenants) {
            try {
                TenantContext.setCurrentTenant(tenant.getId().toString());
                processEventsForTenant(tenant.getId().toString());
            } catch (Exception e) {
                log.error("Error processing events for tenant {}: {}", tenant.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void processEventsForTenant(String tenantId) {
        List<DomainEventEntity> unprocessed = domainEventService.getUnprocessed();

        if (unprocessed.isEmpty()) {
            return;
        }

        log.info("Processing {} unprocessed events for tenant {}", unprocessed.size(), tenantId);

        for (DomainEventEntity event : unprocessed) {
            try {
                List<AgentEventSubscriptionEntity> subscriptions =
                        subscriptionRepository.findByEventTypeAndActiveTrue(event.getEventType());

                for (AgentEventSubscriptionEntity subscription : subscriptions) {
                    try {
                        AgentInstanceEntity instance = agentInstanceService.findById(subscription.getInstanceId());

                        if (instance.getStatus() != AgentInstanceStatus.ACTIVE) {
                            log.debug("Skipping subscription {} - instance {} is not ACTIVE (status={})",
                                    subscription.getId(), instance.getId(), instance.getStatus());
                            continue;
                        }

                        log.info("Triggering run for instance {} (event={}, eventId={})",
                                instance.getName(), event.getEventType(), event.getId());

                        agentRunOrchestrator.triggerRun(
                                instance.getId(),
                                TriggerType.EVENT,
                                event.getEventType(),
                                event.getPayload()
                        );
                    } catch (Exception e) {
                        log.error("Error triggering run for subscription {}: {}",
                                subscription.getId(), e.getMessage(), e);
                    }
                }

                domainEventService.markProcessed(event.getId());
            } catch (Exception e) {
                log.error("Error processing event {}: {}", event.getId(), e.getMessage(), e);
            }
        }
    }
}
