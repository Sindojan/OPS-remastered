package com.owlsburg.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    List<ChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    List<ChatMessageEntity> findBySessionIdAndTenantIdOrderByCreatedAtAsc(UUID sessionId, UUID tenantId);
    void deleteBySessionId(UUID sessionId);
    List<ChatMessageEntity> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after);
}
