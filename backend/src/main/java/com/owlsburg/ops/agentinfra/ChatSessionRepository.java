package com.owlsburg.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {
    List<ChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
