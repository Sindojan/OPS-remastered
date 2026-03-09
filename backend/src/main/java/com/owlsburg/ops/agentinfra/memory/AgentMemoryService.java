package com.owlsburg.ops.agentinfra.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AgentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AgentMemoryService.class);
    private static final int MAX_MEMORIES_PER_AGENT = 200;

    private final AgentMemoryRepository memoryRepository;
    private final ObjectMapper objectMapper;

    public AgentMemoryService(AgentMemoryRepository memoryRepository, ObjectMapper objectMapper) {
        this.memoryRepository = memoryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentMemoryEntity saveMemory(UUID instanceId, String type, String category,
                                         String key, String value, int importance) {
        return saveMemory(instanceId, type, category, key, value, importance, "agent");
    }

    @Transactional
    public AgentMemoryEntity saveMemory(UUID instanceId, String type, String category,
                                         String key, String value, int importance, String source) {
        // Upsert: check if key already exists for this instance
        AgentMemoryEntity existing = memoryRepository.findByInstanceIdAndKey(instanceId, key)
                .orElse(null);

        if (existing != null) {
            existing.setType(type);
            existing.setCategory(category);
            existing.setValue(value);
            existing.setImportance(importance);
            existing.setSource(source);
            existing.setLastAccessedAt(Instant.now());
            applyExpiryDefaults(existing);
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
        memory.setSource(source);
        memory.setLastAccessedAt(Instant.now());
        applyExpiryDefaults(memory);

        AgentMemoryEntity saved = memoryRepository.save(memory);

        // Evict LRU if over limit
        evictLRU(instanceId, MAX_MEMORIES_PER_AGENT);

        log.debug("Saved memory '{}' for instance {}", key, instanceId);
        return saved;
    }

    private void applyExpiryDefaults(AgentMemoryEntity memory) {
        if (memory.getExpiresAt() != null) return; // already set explicitly

        try {
            MemoryType mt = MemoryType.valueOf(memory.getType());
            if (mt.isProcedural() && memory.getImportance() < 8) {
                memory.setExpiresAt(Instant.now().plus(180, ChronoUnit.DAYS));
            } else if (mt.isEpisodic()) {
                memory.setExpiresAt(Instant.now().plus(90, ChronoUnit.DAYS));
            }
            // Semantic + NOTE: no expiry
        } catch (IllegalArgumentException e) {
            // Unknown type, no expiry
        }
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

    private static final List<String> SEMANTIC_TYPES = List.of("FACT", "PREFERENCE", "RULE");
    private static final List<String> PROCEDURAL_TYPES = List.of("LEARNING", "STRATEGY", "WORKFLOW");
    private static final List<String> EPISODIC_TYPES = List.of("DECISION", "EVENT");

    @Transactional
    public String buildMemorySection(UUID instanceId) {
        // Grouped retrieval: 10 semantic + 5 procedural + 5 episodic = 20 total
        List<AgentMemoryEntity> semantic = memoryRepository
                .findByInstanceIdAndTypeInOrderByImportanceDescLastAccessedAtDesc(
                        instanceId, SEMANTIC_TYPES, PageRequest.of(0, 10));
        List<AgentMemoryEntity> procedural = memoryRepository
                .findByInstanceIdAndTypeInOrderByImportanceDescLastAccessedAtDesc(
                        instanceId, PROCEDURAL_TYPES, PageRequest.of(0, 5));
        List<AgentMemoryEntity> episodic = memoryRepository
                .findByInstanceIdAndTypeInOrderByLastAccessedAtDescImportanceDesc(
                        instanceId, EPISODIC_TYPES, PageRequest.of(0, 5));

        if (semantic.isEmpty() && procedural.isEmpty() && episodic.isEmpty()) return "";

        // Update last_accessed_at for all injected memories
        Instant now = Instant.now();
        List<AgentMemoryEntity> all = new java.util.ArrayList<>();
        all.addAll(semantic);
        all.addAll(procedural);
        all.addAll(episodic);
        for (AgentMemoryEntity m : all) {
            m.setLastAccessedAt(now);
        }
        memoryRepository.saveAll(all);

        StringBuilder sb = new StringBuilder("\n\n## Dein Gedächtnis\n");

        if (!semantic.isEmpty()) {
            sb.append("\n### Wissen & Fakten\n");
            for (AgentMemoryEntity m : semantic) {
                sb.append("- [").append(m.getCategory()).append("] ")
                  .append(m.getKey()).append(": ").append(m.getValue()).append("\n");
            }
        }

        if (!procedural.isEmpty()) {
            sb.append("\n### Gelernte Muster\n");
            for (AgentMemoryEntity m : procedural) {
                sb.append("- [").append(m.getCategory()).append("] ")
                  .append(m.getKey()).append(": ").append(m.getValue()).append("\n");
            }
        }

        if (!episodic.isEmpty()) {
            sb.append("\n### Letzte Entscheidungen\n");
            for (AgentMemoryEntity m : episodic) {
                sb.append("- [").append(m.getCategory()).append("] ")
                  .append(m.getKey()).append(": ").append(m.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    @Transactional
    public void recordToolOutcome(UUID instanceId, String toolName, boolean success) {
        String sanitizedToolName = sanitizeKey(toolName);
        String key = "strategy_" + sanitizedToolName;
        AgentMemoryEntity existing = memoryRepository.findByInstanceIdAndKey(instanceId, key)
                .orElse(null);

        if (existing != null) {
            try {
                ObjectNode meta = existing.getMetadata() != null
                        ? (ObjectNode) objectMapper.readTree(existing.getMetadata())
                        : objectMapper.createObjectNode();

                int successCount = meta.path("successCount").asInt(0);
                int failCount = meta.path("failCount").asInt(0);
                if (success) successCount++; else failCount++;

                meta.put("successCount", successCount);
                meta.put("failCount", failCount);
                meta.put("lastUsed", Instant.now().toString());

                existing.setMetadata(objectMapper.writeValueAsString(meta));
                existing.setLastAccessedAt(Instant.now());

                // Auto-adjust importance based on usage frequency
                int total = successCount + failCount;
                if (total >= 5) {
                    double successRate = (double) successCount / total;
                    int newImportance = Math.max(3, Math.min(9, (int) (successRate * 10)));
                    existing.setImportance(newImportance);
                }

                existing.setValue("Tool " + sanitizedToolName + " verwendet (" + successCount + " Erfolge, " + failCount + " Fehler)");
                memoryRepository.save(existing);
            } catch (Exception e) {
                log.warn("Failed to update tool outcome metadata for '{}': {}", sanitizedToolName, e.getMessage());
            }
        } else {
            try {
                ObjectNode meta = objectMapper.createObjectNode();
                meta.put("successCount", success ? 1 : 0);
                meta.put("failCount", success ? 0 : 1);
                meta.put("lastUsed", Instant.now().toString());

                AgentMemoryEntity memory = new AgentMemoryEntity();
                memory.setInstanceId(instanceId);
                memory.setType("STRATEGY");
                memory.setCategory("tool_usage");
                memory.setKey(key);
                memory.setValue("Tool " + sanitizedToolName + " verwendet (" + (success ? "1 Erfolg" : "1 Fehler") + ")");
                memory.setImportance(3);
                memory.setSource("system");
                memory.setLastAccessedAt(Instant.now());
                memory.setMetadata(objectMapper.writeValueAsString(meta));
                applyExpiryDefaults(memory);
                memoryRepository.save(memory);
            } catch (Exception e) {
                log.warn("Failed to create tool outcome memory for '{}': {}", sanitizedToolName, e.getMessage());
            }
        }
    }

    private static String sanitizeKey(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
