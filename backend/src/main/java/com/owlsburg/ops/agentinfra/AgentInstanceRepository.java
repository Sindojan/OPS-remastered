package com.owlsburg.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentInstanceRepository extends JpaRepository<AgentInstanceEntity, UUID> {

    Optional<AgentInstanceEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<AgentInstanceEntity> findByTemplateId(UUID templateId);

    List<AgentInstanceEntity> findByStatus(AgentInstanceStatus status);

    List<AgentInstanceEntity> findByTenantId(UUID tenantId);
}
