package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.agentinfra.AgentRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemAgentRunRepository extends JpaRepository<SystemAgentRunEntity, UUID> {

    List<SystemAgentRunEntity> findByInstanceId(UUID instanceId);

    Page<SystemAgentRunEntity> findByInstanceId(UUID instanceId, Pageable pageable);

    Page<SystemAgentRunEntity> findByInstanceIdOrderByCreatedAtDesc(UUID instanceId, Pageable pageable);

    List<SystemAgentRunEntity> findByStatus(AgentRunStatus status);

    List<SystemAgentRunEntity> findByInstanceIdAndStartedAtBetween(UUID instanceId, Instant from, Instant to);

    Optional<SystemAgentRunEntity> findTopByInstanceIdOrderByStartedAtDesc(UUID instanceId);

    @Query("SELECT COALESCE(SUM(r.tokensUsed), 0) FROM SystemAgentRunEntity r WHERE r.startedAt > :since")
    long sumTokensSince(@Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(r.costUsd), 0) FROM SystemAgentRunEntity r WHERE r.startedAt > :since")
    BigDecimal sumCostSince(@Param("since") Instant since);

    @Query("SELECT COUNT(r) FROM SystemAgentRunEntity r WHERE r.startedAt > :since")
    long countSince(@Param("since") Instant since);

    @Query("SELECT COUNT(r) FROM SystemAgentRunEntity r WHERE r.status = 'FAILED' AND r.startedAt > :since")
    long countFailedSince(@Param("since") Instant since);
}
