package com.owlsburg.ops.agentinfra.messaging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, UUID> {

    List<AgentMessageEntity> findByTargetInstanceIdAndStatusOrderByCreatedAtAsc(UUID targetInstanceId, String status);

    List<AgentMessageEntity> findBySenderInstanceId(UUID senderInstanceId);

    List<AgentMessageEntity> findByTargetInstanceIdOrderByCreatedAtDesc(UUID targetInstanceId, Pageable pageable);

    List<AgentMessageEntity> findByCreatedAtAfter(Instant after);
}
