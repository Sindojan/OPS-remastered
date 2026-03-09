package com.owlsburg.ops.agentinfra.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentMemoryServiceTest {

    @Mock
    private AgentMemoryRepository memoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AgentMemoryService service;

    private final UUID instanceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AgentMemoryService(memoryRepository, objectMapper);
    }

    @Test
    void saveMemory_newEntry_setsExpiryForEpisodicType() {
        when(memoryRepository.findByInstanceIdAndKey(any(), any())).thenReturn(Optional.empty());
        when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memoryRepository.countByInstanceId(any())).thenReturn(1L);

        AgentMemoryEntity result = service.saveMemory(instanceId, "EVENT", "test", "key1", "value1", 5);

        assertNotNull(result.getExpiresAt());
        // EVENT is episodic → 90 days expiry
        assertTrue(result.getExpiresAt().isAfter(Instant.now().plusSeconds(89 * 86400)));
    }

    @Test
    void saveMemory_newEntry_noExpiryForSemanticType() {
        when(memoryRepository.findByInstanceIdAndKey(any(), any())).thenReturn(Optional.empty());
        when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memoryRepository.countByInstanceId(any())).thenReturn(1L);

        AgentMemoryEntity result = service.saveMemory(instanceId, "FACT", "test", "key1", "value1", 5);

        assertNull(result.getExpiresAt());
    }

    @Test
    void saveMemory_newEntry_expiryForProceduralLowImportance() {
        when(memoryRepository.findByInstanceIdAndKey(any(), any())).thenReturn(Optional.empty());
        when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memoryRepository.countByInstanceId(any())).thenReturn(1L);

        AgentMemoryEntity result = service.saveMemory(instanceId, "LEARNING", "test", "key1", "value1", 5);

        assertNotNull(result.getExpiresAt());
        // LEARNING is procedural, importance < 8 → 180 days expiry
        assertTrue(result.getExpiresAt().isAfter(Instant.now().plusSeconds(179 * 86400)));
    }

    @Test
    void saveMemory_newEntry_noExpiryForProceduralHighImportance() {
        when(memoryRepository.findByInstanceIdAndKey(any(), any())).thenReturn(Optional.empty());
        when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memoryRepository.countByInstanceId(any())).thenReturn(1L);

        AgentMemoryEntity result = service.saveMemory(instanceId, "STRATEGY", "test", "key1", "value1", 9);

        assertNull(result.getExpiresAt());
    }

    @Test
    void saveMemory_withSource() {
        when(memoryRepository.findByInstanceIdAndKey(any(), any())).thenReturn(Optional.empty());
        when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memoryRepository.countByInstanceId(any())).thenReturn(1L);

        AgentMemoryEntity result = service.saveMemory(instanceId, "EVENT", "test", "key1", "value1", 5, "system");

        assertEquals("system", result.getSource());
    }

    @Test
    void buildMemorySection_groupedByType() {
        AgentMemoryEntity fact = createMemory("FACT", "domain", "fact_key", "fact_value", 8);
        AgentMemoryEntity strategy = createMemory("STRATEGY", "tools", "strat_key", "strat_value", 6);
        AgentMemoryEntity decision = createMemory("DECISION", "delegation", "dec_key", "dec_value", 5);

        when(memoryRepository.findByInstanceIdAndTypeInOrderByImportanceDescLastAccessedAtDesc(
                eq(instanceId), eq(List.of("FACT", "PREFERENCE", "RULE")), any(PageRequest.class)))
                .thenReturn(List.of(fact));
        when(memoryRepository.findByInstanceIdAndTypeInOrderByImportanceDescLastAccessedAtDesc(
                eq(instanceId), eq(List.of("LEARNING", "STRATEGY", "WORKFLOW")), any(PageRequest.class)))
                .thenReturn(List.of(strategy));
        when(memoryRepository.findByInstanceIdAndTypeInOrderByLastAccessedAtDescImportanceDesc(
                eq(instanceId), eq(List.of("DECISION", "EVENT")), any(PageRequest.class)))
                .thenReturn(List.of(decision));

        String section = service.buildMemorySection(instanceId);

        assertTrue(section.contains("### Wissen & Fakten"));
        assertTrue(section.contains("fact_key: fact_value"));
        assertTrue(section.contains("### Gelernte Muster"));
        assertTrue(section.contains("strat_key: strat_value"));
        assertTrue(section.contains("### Letzte Entscheidungen"));
        assertTrue(section.contains("dec_key: dec_value"));
    }

    @Test
    void buildMemorySection_emptyReturnsEmptyString() {
        when(memoryRepository.findByInstanceIdAndTypeInOrderByImportanceDescLastAccessedAtDesc(
                any(), any(), any())).thenReturn(List.of());
        when(memoryRepository.findByInstanceIdAndTypeInOrderByLastAccessedAtDescImportanceDesc(
                any(), any(), any())).thenReturn(List.of());

        String section = service.buildMemorySection(instanceId);

        assertEquals("", section);
    }

    @Test
    void recordToolOutcome_newEntry() {
        when(memoryRepository.findByInstanceIdAndKey(eq(instanceId), eq("strategy_get_jobs")))
                .thenReturn(Optional.empty());
        when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordToolOutcome(instanceId, "get_jobs", true);

        ArgumentCaptor<AgentMemoryEntity> captor = ArgumentCaptor.forClass(AgentMemoryEntity.class);
        verify(memoryRepository).save(captor.capture());

        AgentMemoryEntity saved = captor.getValue();
        assertEquals("STRATEGY", saved.getType());
        assertEquals("tool_usage", saved.getCategory());
        assertEquals("strategy_get_jobs", saved.getKey());
        assertEquals("system", saved.getSource());
        assertTrue(saved.getMetadata().contains("\"successCount\":1"));
    }

    @Test
    void recordToolOutcome_updatesExisting() {
        AgentMemoryEntity existing = createMemory("STRATEGY", "tool_usage", "strategy_get_jobs", "old", 3);
        existing.setMetadata("{\"successCount\":3,\"failCount\":1,\"lastUsed\":\"2024-01-01T00:00:00Z\"}");

        when(memoryRepository.findByInstanceIdAndKey(eq(instanceId), eq("strategy_get_jobs")))
                .thenReturn(Optional.of(existing));
        when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordToolOutcome(instanceId, "get_jobs", true);

        verify(memoryRepository).save(existing);
        assertTrue(existing.getMetadata().contains("\"successCount\":4"));
        assertTrue(existing.getMetadata().contains("\"failCount\":1"));
        // 5 total calls, 80% success rate → importance ~8
        assertTrue(existing.getImportance() >= 7);
    }

    private AgentMemoryEntity createMemory(String type, String category, String key, String value, int importance) {
        AgentMemoryEntity m = new AgentMemoryEntity();
        m.setInstanceId(instanceId);
        m.setType(type);
        m.setCategory(category);
        m.setKey(key);
        m.setValue(value);
        m.setImportance(importance);
        m.setLastAccessedAt(Instant.now());
        m.setSource("agent");
        m.setMetadata("{}");
        return m;
    }
}
