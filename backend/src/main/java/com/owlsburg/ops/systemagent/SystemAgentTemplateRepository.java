package com.owlsburg.ops.systemagent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemAgentTemplateRepository extends JpaRepository<SystemAgentTemplateEntity, UUID> {

    Optional<SystemAgentTemplateEntity> findByRole(String role);
}
