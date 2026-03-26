package com.owlsburg.ops.systemagent.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SystemAgentMessageRepository extends JpaRepository<SystemAgentMessageEntity, UUID> {

    List<SystemAgentMessageEntity> findByTargetInstanceIdAndStatusOrderByCreatedAtAsc(UUID targetInstanceId, String status);

    List<SystemAgentMessageEntity> findByTargetInstanceIdOrderByCreatedAtDesc(UUID targetInstanceId);

    List<SystemAgentMessageEntity> findBySenderInstanceIdOrderByCreatedAtDesc(UUID senderInstanceId);

    List<SystemAgentMessageEntity> findByCreatedAtAfter(Instant after);
}
