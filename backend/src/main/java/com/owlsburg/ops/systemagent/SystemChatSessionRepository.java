package com.owlsburg.ops.systemagent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SystemChatSessionRepository extends JpaRepository<SystemChatSessionEntity, UUID> {

    List<SystemChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<SystemChatSessionEntity> findByUserIdAndAgentInstanceIdOrderByUpdatedAtDesc(UUID userId, UUID agentInstanceId);

    List<SystemChatSessionEntity> findByIdIn(Collection<UUID> ids);
}
