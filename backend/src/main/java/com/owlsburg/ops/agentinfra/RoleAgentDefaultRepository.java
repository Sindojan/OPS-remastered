package com.owlsburg.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleAgentDefaultRepository extends JpaRepository<RoleAgentDefaultEntity, UUID> {

    Optional<RoleAgentDefaultEntity> findByRole(String role);

    List<RoleAgentDefaultEntity> findAll();
}
