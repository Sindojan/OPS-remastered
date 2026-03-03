package com.owlsburg.ops.agentinfra.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MemoryPruningScheduler {

    private static final Logger log = LoggerFactory.getLogger(MemoryPruningScheduler.class);

    private final AgentMemoryService memoryService;

    public MemoryPruningScheduler(AgentMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Scheduled(cron = "0 0 3 * * *") // Daily at 3:00 AM
    public void pruneExpiredMemories() {
        int deleted = memoryService.pruneExpired();
        if (deleted > 0) {
            log.info("Daily memory pruning: removed {} expired entries", deleted);
        }
    }
}
