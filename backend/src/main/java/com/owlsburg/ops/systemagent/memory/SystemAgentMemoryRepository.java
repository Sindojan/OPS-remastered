package com.owlsburg.ops.systemagent.memory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemAgentMemoryRepository extends JpaRepository<SystemAgentMemoryEntity, UUID> {

    Optional<SystemAgentMemoryEntity> findByInstanceIdAndCategoryAndKey(UUID instanceId, String category, String key);

    List<SystemAgentMemoryEntity> findByInstanceIdAndCategoryOrderByImportanceDesc(UUID instanceId, String category);

    List<SystemAgentMemoryEntity> findByInstanceIdOrderByImportanceDesc(UUID instanceId, Pageable pageable);

    List<SystemAgentMemoryEntity> findByInstanceIdAndTypeOrderByImportanceDesc(UUID instanceId, String type, Pageable pageable);

    @Query("SELECT m FROM SystemAgentMemoryEntity m WHERE m.instanceId = :instanceId ORDER BY m.lastAccessedAt ASC")
    List<SystemAgentMemoryEntity> findByInstanceIdOrderByLastAccessedAtAsc(@Param("instanceId") UUID instanceId);

    long countByInstanceId(UUID instanceId);

    @Modifying
    @Query("DELETE FROM SystemAgentMemoryEntity m WHERE m.expiresAt IS NOT NULL AND m.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
