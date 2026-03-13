package com.owlsburg.ops.systemagent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemAgentIncidentRepository extends JpaRepository<SystemAgentIncidentEntity, UUID> {

    List<SystemAgentIncidentEntity> findByInstanceIdOrderByCreatedAtDesc(UUID instanceId);

    List<SystemAgentIncidentEntity> findByInstanceIdAndResolvedAtIsNull(UUID instanceId);

    long countByResolvedAtIsNull();
}
