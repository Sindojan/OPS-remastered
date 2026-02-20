package com.sindoflow.ops.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledTriggerRepository extends JpaRepository<ScheduledTriggerEntity, UUID> {

    List<ScheduledTriggerEntity> findByActiveTrue();

    List<ScheduledTriggerEntity> findByInstanceId(UUID instanceId);

    List<ScheduledTriggerEntity> findByNextRunAtBeforeAndActiveTrue(Instant now);
}
