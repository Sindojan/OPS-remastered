package com.sindoflow.ops.agentinfra.llm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantLlmConfigRepository extends JpaRepository<TenantLlmConfigEntity, UUID> {

    Optional<TenantLlmConfigEntity> findFirstByOrderByCreatedAtAsc();
}
