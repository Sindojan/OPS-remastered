package com.owlsburg.ops.agentinfra.runtime;

import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentInstanceRepository;
import com.owlsburg.ops.agentinfra.AgentInstanceStatus;
import com.owlsburg.ops.agentinfra.AgentInstanceType;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class SubAgentCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubAgentCleanupScheduler.class);

    private final AgentInstanceRepository instanceRepository;
    private final AgentMemoryService memoryService;

    public SubAgentCleanupScheduler(AgentInstanceRepository instanceRepository,
                                     AgentMemoryService memoryService) {
        this.instanceRepository = instanceRepository;
        this.memoryService = memoryService;
    }

    @Scheduled(initialDelay = 60_000, fixedRate = 1_800_000) // 1min delay, then every 30 minutes
    public void cleanupExpiredSubAgents() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);

        List<AgentInstanceEntity> expired = instanceRepository.findByStatus(AgentInstanceStatus.ACTIVE)
                .stream()
                .filter(i -> i.getType() == AgentInstanceType.EPHEMERAL)
                .filter(i -> i.getUpdatedAt().isBefore(cutoff))
                .toList();

        if (expired.isEmpty()) return;

        log.info("Cleaning up {} expired sub-agents", expired.size());

        for (AgentInstanceEntity subAgent : expired) {
            try {
                // Promote important memories to parent
                if (subAgent.getParentInstanceId() != null) {
                    memoryService.promoteMemories(
                            subAgent.getId(), subAgent.getParentInstanceId(), 7);
                }

                // Delete remaining memories
                memoryService.deleteAllForInstance(subAgent.getId());

                // Terminate
                subAgent.setStatus(AgentInstanceStatus.TERMINATED);
                subAgent.setTerminatedAt(Instant.now());
                instanceRepository.save(subAgent);

                log.info("Terminated expired sub-agent: {} ({})",
                        subAgent.getName(), subAgent.getId());
            } catch (Exception e) {
                log.error("Error cleaning up sub-agent {}: {}",
                        subAgent.getId(), e.getMessage());
            }
        }
    }
}
