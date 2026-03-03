package com.owlsburg.ops.agentinfra.memory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentMemoryRepository extends JpaRepository<AgentMemoryEntity, UUID> {

    List<AgentMemoryEntity> findByInstanceIdOrderByImportanceDescLastAccessedAtDesc(UUID instanceId, Pageable pageable);

    List<AgentMemoryEntity> findByInstanceIdAndCategoryOrderByImportanceDesc(UUID instanceId, String category);

    Optional<AgentMemoryEntity> findByInstanceIdAndKey(UUID instanceId, String key);

    long countByInstanceId(UUID instanceId);

    List<AgentMemoryEntity> findByInstanceIdOrderByImportanceAscLastAccessedAtAsc(UUID instanceId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AgentMemoryEntity m WHERE m.expiresAt IS NOT NULL AND m.expiresAt < :now")
    int deleteExpiredMemories(Instant now);

    List<AgentMemoryEntity> findByInstanceIdAndImportanceGreaterThanEqual(UUID instanceId, int minImportance);

    @Modifying
    @Query("DELETE FROM AgentMemoryEntity m WHERE m.instanceId = :instanceId")
    int deleteAllByInstanceId(UUID instanceId);
}
