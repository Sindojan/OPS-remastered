package com.sindoflow.ops.agentinfra.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentEventSubscriptionRepository extends JpaRepository<AgentEventSubscriptionEntity, UUID> {

    List<AgentEventSubscriptionEntity> findByEventTypeAndActiveTrue(String eventType);

    List<AgentEventSubscriptionEntity> findByInstanceId(UUID instanceId);
}
