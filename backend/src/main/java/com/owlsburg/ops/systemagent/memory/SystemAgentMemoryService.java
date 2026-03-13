package com.owlsburg.ops.systemagent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SystemAgentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentMemoryService.class);
    private static final int MAX_MEMORIES_PER_INSTANCE = 200;

    private final SystemAgentMemoryRepository memoryRepository;

    public SystemAgentMemoryService(SystemAgentMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Transactional
    public SystemAgentMemoryEntity saveMemory(UUID instanceId, String type, String category,
                                               String key, String value, int importance) {
        Optional<SystemAgentMemoryEntity> existing =
                memoryRepository.findByInstanceIdAndCategoryAndKey(instanceId, category, key);

        SystemAgentMemoryEntity memory;
        if (existing.isPresent()) {
            memory = existing.get();
            memory.setValue(value);
            memory.setImportance(importance);
            memory.setLastAccessedAt(Instant.now());
        } else {
            memory = new SystemAgentMemoryEntity();
            memory.setInstanceId(instanceId);
            memory.setType(type);
            memory.setCategory(category);
            memory.setKey(key);
            memory.setValue(value);
            memory.setImportance(importance);
            memory.setLastAccessedAt(Instant.now());

            evictLRU(instanceId, MAX_MEMORIES_PER_INSTANCE);
        }

        return memoryRepository.save(memory);
    }

    @Transactional(readOnly = true)
    public List<SystemAgentMemoryEntity> recallMemories(UUID instanceId, String category, int limit) {
        if (category != null && !category.isBlank()) {
            return memoryRepository.findByInstanceIdAndCategoryOrderByImportanceDesc(instanceId, category)
                    .stream().limit(limit).toList();
        }
        return memoryRepository.findByInstanceIdOrderByImportanceDesc(instanceId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public String buildMemorySection(UUID instanceId) {
        List<SystemAgentMemoryEntity> semantic = memoryRepository
                .findByInstanceIdAndTypeOrderByImportanceDesc(instanceId, "SEMANTIC", PageRequest.of(0, 10));
        List<SystemAgentMemoryEntity> procedural = memoryRepository
                .findByInstanceIdAndTypeOrderByImportanceDesc(instanceId, "PROCEDURAL", PageRequest.of(0, 5));
        List<SystemAgentMemoryEntity> episodic = memoryRepository
                .findByInstanceIdAndTypeOrderByImportanceDesc(instanceId, "EPISODIC", PageRequest.of(0, 5));

        if (semantic.isEmpty() && procedural.isEmpty() && episodic.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n\n## Erinnerungen\n");

        if (!semantic.isEmpty()) {
            sb.append("\n### Wissen\n");
            for (SystemAgentMemoryEntity m : semantic) {
                sb.append("- [").append(m.getCategory()).append("] ").append(m.getKey())
                        .append(": ").append(m.getValue()).append("\n");
            }
        }
        if (!procedural.isEmpty()) {
            sb.append("\n### Gelerntes\n");
            for (SystemAgentMemoryEntity m : procedural) {
                sb.append("- ").append(m.getKey()).append(": ").append(m.getValue()).append("\n");
            }
        }
        if (!episodic.isEmpty()) {
            sb.append("\n### Letzte Ereignisse\n");
            for (SystemAgentMemoryEntity m : episodic) {
                sb.append("- ").append(m.getKey()).append(": ").append(m.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    @Transactional
    public void recordToolOutcome(UUID instanceId, String toolName, boolean success) {
        String key = "tool_outcome:" + toolName;
        Optional<SystemAgentMemoryEntity> existing =
                memoryRepository.findByInstanceIdAndCategoryAndKey(instanceId, "tool_outcomes", key);

        if (existing.isPresent()) {
            SystemAgentMemoryEntity m = existing.get();
            m.setValue(success ? "success" : "failure");
            m.setLastAccessedAt(Instant.now());
            memoryRepository.save(m);
        } else {
            SystemAgentMemoryEntity m = new SystemAgentMemoryEntity();
            m.setInstanceId(instanceId);
            m.setType("PROCEDURAL");
            m.setCategory("tool_outcomes");
            m.setKey(key);
            m.setValue(success ? "success" : "failure");
            m.setImportance(3);
            m.setLastAccessedAt(Instant.now());
            memoryRepository.save(m);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void pruneExpired() {
        int deleted = memoryRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Pruned {} expired system agent memories", deleted);
        }
    }

    private void evictLRU(UUID instanceId, int maxEntries) {
        long count = memoryRepository.countByInstanceId(instanceId);
        if (count < maxEntries) return;

        List<SystemAgentMemoryEntity> lruList =
                memoryRepository.findByInstanceIdOrderByLastAccessedAtAsc(instanceId);
        int toEvict = (int) (count - maxEntries + 1);
        for (int i = 0; i < toEvict && i < lruList.size(); i++) {
            memoryRepository.delete(lruList.get(i));
        }
        log.debug("Evicted {} LRU system memories for instance {}", toEvict, instanceId);
    }
}
