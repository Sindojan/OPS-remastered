package com.owlsburg.ops.agentinfra.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AgentMemoryService.class);
    private static final int MAX_MEMORIES_PER_AGENT = 200;

    private final AgentMemoryRepository memoryRepository;

    public AgentMemoryService(AgentMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Transactional
    public AgentMemoryEntity saveMemory(UUID instanceId, String type, String category,
                                         String key, String value, int importance) {
        // Upsert: check if key already exists for this instance
        AgentMemoryEntity existing = memoryRepository.findByInstanceIdAndKey(instanceId, key)
                .orElse(null);

        if (existing != null) {
            existing.setType(type);
            existing.setCategory(category);
            existing.setValue(value);
            existing.setImportance(importance);
            existing.setLastAccessedAt(Instant.now());
            log.debug("Updated memory '{}' for instance {}", key, instanceId);
            return memoryRepository.save(existing);
        }

        AgentMemoryEntity memory = new AgentMemoryEntity();
        memory.setInstanceId(instanceId);
        memory.setType(type);
        memory.setCategory(category);
        memory.setKey(key);
        memory.setValue(value);
        memory.setImportance(importance);
        memory.setLastAccessedAt(Instant.now());

        AgentMemoryEntity saved = memoryRepository.save(memory);

        // Evict LRU if over limit
        evictLRU(instanceId, MAX_MEMORIES_PER_AGENT);

        log.debug("Saved memory '{}' for instance {}", key, instanceId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AgentMemoryEntity> recallMemories(UUID instanceId, String category, int limit) {
        if (category != null && !category.isBlank()) {
            List<AgentMemoryEntity> memories = memoryRepository
                    .findByInstanceIdAndCategoryOrderByImportanceDesc(instanceId, category);
            return memories.stream().limit(limit).toList();
        }
        return memoryRepository.findByInstanceIdOrderByImportanceDescLastAccessedAtDesc(
                instanceId, PageRequest.of(0, limit));
    }

    @Transactional
    public List<AgentMemoryEntity> getTopMemories(UUID instanceId, int limit) {
        List<AgentMemoryEntity> memories = memoryRepository
                .findByInstanceIdOrderByImportanceDescLastAccessedAtDesc(
                        instanceId, PageRequest.of(0, limit));

        // Update last_accessed_at for injected memories
        for (AgentMemoryEntity m : memories) {
            m.setLastAccessedAt(Instant.now());
        }
        memoryRepository.saveAll(memories);

        return memories;
    }

    @Transactional(readOnly = true)
    public List<AgentMemoryEntity> readAgentMemories(UUID targetInstanceId) {
        return memoryRepository.findByInstanceIdOrderByImportanceDescLastAccessedAtDesc(
                targetInstanceId, PageRequest.of(0, 20));
    }

    @Transactional
    public int pruneExpired() {
        int deleted = memoryRepository.deleteExpiredMemories(Instant.now());
        if (deleted > 0) {
            log.info("Pruned {} expired agent memories", deleted);
        }
        return deleted;
    }

    @Transactional
    public void evictLRU(UUID instanceId, int maxEntries) {
        long count = memoryRepository.countByInstanceId(instanceId);
        if (count <= maxEntries) return;

        int excess = (int) (count - maxEntries);
        List<AgentMemoryEntity> toEvict = memoryRepository
                .findByInstanceIdOrderByImportanceAscLastAccessedAtAsc(
                        instanceId, PageRequest.of(0, excess));

        memoryRepository.deleteAll(toEvict);
        log.debug("Evicted {} LRU memories for instance {}", toEvict.size(), instanceId);
    }

    @Transactional
    public void promoteMemories(UUID sourceInstanceId, UUID targetInstanceId, int minImportance) {
        List<AgentMemoryEntity> important = memoryRepository
                .findByInstanceIdAndImportanceGreaterThanEqual(sourceInstanceId, minImportance);

        for (AgentMemoryEntity m : important) {
            saveMemory(targetInstanceId, m.getType(), "sub_agent:" + m.getCategory(),
                    m.getKey(), m.getValue(), m.getImportance());
        }

        log.info("Promoted {} memories from instance {} to {}",
                important.size(), sourceInstanceId, targetInstanceId);
    }

    @Transactional
    public int deleteAllForInstance(UUID instanceId) {
        return memoryRepository.deleteAllByInstanceId(instanceId);
    }

    public String buildMemorySection(UUID instanceId) {
        List<AgentMemoryEntity> memories = getTopMemories(instanceId, 20);
        if (memories.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("\n\n## Dein Gedächtnis\n");
        for (AgentMemoryEntity m : memories) {
            sb.append("- **[").append(m.getCategory()).append("]** ")
              .append(m.getKey()).append(": ").append(m.getValue()).append("\n");
        }
        return sb.toString();
    }
}
