package com.owlsburg.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentTemplateRepository extends JpaRepository<AgentTemplateEntity, UUID> {

    Optional<AgentTemplateEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<AgentTemplateEntity> findByStatus(String status);

    List<AgentTemplateEntity> findByRole(String role);

    Optional<AgentTemplateEntity> findByRoleAndTenantId(String role, UUID tenantId);
}
