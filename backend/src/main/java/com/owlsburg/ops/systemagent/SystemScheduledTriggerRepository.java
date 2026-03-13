package com.owlsburg.ops.systemagent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemScheduledTriggerRepository extends JpaRepository<SystemScheduledTriggerEntity, UUID> {

    List<SystemScheduledTriggerEntity> findByEnabledTrue();

    List<SystemScheduledTriggerEntity> findByInstanceId(UUID instanceId);
}
