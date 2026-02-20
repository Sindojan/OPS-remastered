package com.sindoflow.ops.agentinfra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentRunStepRepository extends JpaRepository<AgentRunStepEntity, UUID> {

    List<AgentRunStepEntity> findByRunIdOrderByStepNumber(UUID runId);
}
