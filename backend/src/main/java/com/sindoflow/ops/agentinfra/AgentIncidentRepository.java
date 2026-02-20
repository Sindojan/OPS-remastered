package com.sindoflow.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentIncidentRepository extends JpaRepository<AgentIncidentEntity, UUID> {

    List<AgentIncidentEntity> findByInstanceId(UUID instanceId);

    List<AgentIncidentEntity> findByResolvedAtIsNull();
}
