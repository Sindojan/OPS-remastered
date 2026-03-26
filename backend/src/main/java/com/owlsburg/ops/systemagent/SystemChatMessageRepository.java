package com.owlsburg.ops.systemagent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SystemChatMessageRepository extends JpaRepository<SystemChatMessageEntity, UUID> {

    List<SystemChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    List<SystemChatMessageEntity> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after);

    void deleteBySessionId(UUID sessionId);
}
