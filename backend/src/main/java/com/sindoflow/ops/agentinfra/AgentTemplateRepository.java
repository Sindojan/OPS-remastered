package com.sindoflow.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentTemplateRepository extends JpaRepository<AgentTemplateEntity, UUID> {

    List<AgentTemplateEntity> findByStatus(String status);

    List<AgentTemplateEntity> findByRole(String role);
}
