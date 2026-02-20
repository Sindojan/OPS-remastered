package com.sindoflow.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentInstanceRepository extends JpaRepository<AgentInstanceEntity, UUID> {

    List<AgentInstanceEntity> findByTemplateId(UUID templateId);

    List<AgentInstanceEntity> findByStatus(AgentInstanceStatus status);

    List<AgentInstanceEntity> findByTenantId(String tenantId);
}
