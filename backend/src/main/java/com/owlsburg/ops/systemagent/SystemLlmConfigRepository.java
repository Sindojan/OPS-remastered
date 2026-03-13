package com.owlsburg.ops.systemagent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SystemLlmConfigRepository extends JpaRepository<SystemLlmConfigEntity, UUID> {
}
