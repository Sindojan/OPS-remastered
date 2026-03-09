package com.owlsburg.ops.agentinfra;

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
public interface AgentRunRepository extends JpaRepository<AgentRunEntity, UUID> {

    List<AgentRunEntity> findByInstanceId(UUID instanceId);

    List<AgentRunEntity> findByStatus(AgentRunStatus status);

    List<AgentRunEntity> findByInstanceIdAndStartedAtBetween(UUID instanceId, Instant from, Instant to);

    List<AgentRunEntity> findByStartedAtBetween(Instant from, Instant to);

    Optional<AgentRunEntity> findTopByInstanceIdOrderByStartedAtDesc(UUID instanceId);

    // ─── Native queries for System Admin tenant aggregation (bypasses RLS) ───

    @Query(value = "SELECT COUNT(*) FROM agent_runs ar JOIN agent_instances ai ON ar.instance_id = ai.id WHERE ai.tenant_id = :tenantId AND ar.started_at > :since", nativeQuery = true)
    long countByTenantSince(@Param("tenantId") UUID tenantId, @Param("since") Instant since);

    @Query(value = "SELECT COALESCE(SUM(ar.tokens_used), 0) FROM agent_runs ar JOIN agent_instances ai ON ar.instance_id = ai.id WHERE ai.tenant_id = :tenantId AND ar.started_at > :since", nativeQuery = true)
    long sumTokensByTenantSince(@Param("tenantId") UUID tenantId, @Param("since") Instant since);

    @Query(value = "SELECT COALESCE(SUM(ar.cost_usd), 0) FROM agent_runs ar JOIN agent_instances ai ON ar.instance_id = ai.id WHERE ai.tenant_id = :tenantId AND ar.started_at > :since", nativeQuery = true)
    BigDecimal sumCostByTenantSince(@Param("tenantId") UUID tenantId, @Param("since") Instant since);

    @Query(value = "SELECT ar.started_at FROM agent_runs ar JOIN agent_instances ai ON ar.instance_id = ai.id WHERE ai.tenant_id = :tenantId ORDER BY ar.started_at DESC LIMIT 1", nativeQuery = true)
    Instant findLastActiveByTenant(@Param("tenantId") UUID tenantId);
}
