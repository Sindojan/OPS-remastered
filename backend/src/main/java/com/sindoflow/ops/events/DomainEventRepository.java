package com.sindoflow.ops.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DomainEventRepository extends JpaRepository<DomainEventEntity, UUID> {

    List<DomainEventEntity> findByProcessedFalse();

    List<DomainEventEntity> findByEventType(String eventType);

    List<DomainEventEntity> findBySourceTypeAndSourceId(String sourceType, UUID sourceId);
}
