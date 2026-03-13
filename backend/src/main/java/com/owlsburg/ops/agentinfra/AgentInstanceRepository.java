package com.owlsburg.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    List<AgentInstanceEntity> findByActivityStatus(AgentActivityStatus activityStatus);

    @Modifying
    @Query(value = "UPDATE agent_instances SET activity_status = 'IDLE', activity_status_changed_at = NOW() " +
            "WHERE activity_status = 'BUSY'", nativeQuery = true)
    int resetAllBusyToIdle();
}
