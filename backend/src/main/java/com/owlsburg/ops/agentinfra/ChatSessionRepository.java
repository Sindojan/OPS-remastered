package com.owlsburg.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {
    Optional<ChatSessionEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    List<ChatSessionEntity> findByAgentInstanceId(UUID agentInstanceId);
}
