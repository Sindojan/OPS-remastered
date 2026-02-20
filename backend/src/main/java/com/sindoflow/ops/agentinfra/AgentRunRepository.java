package com.sindoflow.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRunEntity, UUID> {

    List<AgentRunEntity> findByInstanceId(UUID instanceId);

    List<AgentRunEntity> findByStatus(AgentRunStatus status);

    List<AgentRunEntity> findByInstanceIdAndStartedAtBetween(UUID instanceId, Instant from, Instant to);
}
