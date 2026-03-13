package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.agentinfra.AgentActivityStatus;
import com.owlsburg.ops.agentinfra.AgentInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemAgentInstanceRepository extends JpaRepository<SystemAgentInstanceEntity, UUID> {

    List<SystemAgentInstanceEntity> findByTemplateId(UUID templateId);

    List<SystemAgentInstanceEntity> findByStatus(AgentInstanceStatus status);

    List<SystemAgentInstanceEntity> findByActivityStatus(AgentActivityStatus activityStatus);

    List<SystemAgentInstanceEntity> findByParentInstanceId(UUID parentInstanceId);

    @Modifying
    @Query(value = "UPDATE system_agent_instances SET activity_status = 'IDLE', activity_status_changed_at = NOW() " +
            "WHERE activity_status = 'BUSY'", nativeQuery = true)
    int resetAllBusyToIdle();
}
