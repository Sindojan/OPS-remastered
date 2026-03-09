package com.owlsburg.ops.agentinfra.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpisodicMemoryExtractorTest {

    @Mock
    private AgentMemoryService memoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EpisodicMemoryExtractor extractor;

    private final UUID instanceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        extractor = new EpisodicMemoryExtractor(memoryService, objectMapper);
    }

    @Test
    void extractFromToolCalls_delegation_createsDecisionMemory() {
        var toolCalls = List.of(
                new EpisodicMemoryExtractor.ToolCallRecord(
                        "delegate_to_lead",
                        "{\"lead\":\"production_lead\",\"task\":\"Schichtplan prüfen\"}",
                        "{\"output\":\"Erledigt\"}",
                        true)
        );

        when(memoryService.saveMemory(any(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(new AgentMemoryEntity());

        extractor.extractFromToolCalls(instanceId, toolCalls);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> categoryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sourceCaptor = ArgumentCaptor.forClass(String.class);

        verify(memoryService).saveMemory(eq(instanceId), typeCaptor.capture(), categoryCaptor.capture(),
                anyString(), valueCaptor.capture(), eq(6), sourceCaptor.capture());

        assertEquals("DECISION", typeCaptor.getValue());
        assertEquals("delegation", categoryCaptor.getValue());
        assertTrue(valueCaptor.getValue().contains("production_lead"));
        assertTrue(valueCaptor.getValue().contains("Schichtplan prüfen"));
        assertEquals("system", sourceCaptor.getValue());
    }

    @Test
    void extractFromToolCalls_statusChange_createsEventMemory() {
        var toolCalls = List.of(
                new EpisodicMemoryExtractor.ToolCallRecord(
                        "update_job_status",
                        "{\"jobId\":\"123\",\"status\":\"QUALITY\"}",
                        "{\"success\":true}",
                        true)
        );

        when(memoryService.saveMemory(any(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(new AgentMemoryEntity());

        extractor.extractFromToolCalls(instanceId, toolCalls);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(memoryService).saveMemory(eq(instanceId), typeCaptor.capture(), eq("tool_action"),
                anyString(), anyString(), eq(4), eq("system"));

        assertEquals("EVENT", typeCaptor.getValue());
    }

    @Test
    void extractFromToolCalls_readOnlyTool_noMemory() {
        var toolCalls = List.of(
                new EpisodicMemoryExtractor.ToolCallRecord(
                        "get_jobs",
                        "{}",
                        "[{\"id\":1}]",
                        true)
        );

        extractor.extractFromToolCalls(instanceId, toolCalls);

        verifyNoInteractions(memoryService);
    }

    @Test
    void extractFromToolCalls_emptyList_noOp() {
        extractor.extractFromToolCalls(instanceId, List.of());
        verifyNoInteractions(memoryService);
    }

    @Test
    void extractFromToolCalls_nullList_noOp() {
        extractor.extractFromToolCalls(instanceId, null);
        verifyNoInteractions(memoryService);
    }

    @Test
    void extractFromToolCalls_failedStatusChange_noMemory() {
        var toolCalls = List.of(
                new EpisodicMemoryExtractor.ToolCallRecord(
                        "update_job_status",
                        "{\"jobId\":\"123\",\"status\":\"QUALITY\"}",
                        "Fehler: Job nicht gefunden",
                        false)
        );

        extractor.extractFromToolCalls(instanceId, toolCalls);

        verifyNoInteractions(memoryService);
    }
}
