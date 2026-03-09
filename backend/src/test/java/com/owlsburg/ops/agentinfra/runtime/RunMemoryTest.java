package com.owlsburg.ops.agentinfra.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RunMemoryTest {

    private RunMemory runMemory;

    @BeforeEach
    void setUp() {
        runMemory = new RunMemory();
    }

    @Test
    void putAndGet() {
        runMemory.put("key1", "value1");
        assertEquals("value1", runMemory.get("key1"));
    }

    @Test
    void getReturnsNullForMissing() {
        assertNull(runMemory.get("nonexistent"));
    }

    @Test
    void isEmpty() {
        assertTrue(runMemory.isEmpty());
        runMemory.put("key", "value");
        assertFalse(runMemory.isEmpty());
    }

    @Test
    void clear() {
        runMemory.put("key", "value");
        runMemory.clear();
        assertTrue(runMemory.isEmpty());
        assertNull(runMemory.get("key"));
    }

    @Test
    void getAllReturnsUnmodifiableMap() {
        runMemory.put("a", "1");
        runMemory.put("b", "2");
        var all = runMemory.getAll();
        assertEquals(2, all.size());
        assertThrows(UnsupportedOperationException.class, () -> all.put("c", "3"));
    }

    @Test
    void buildSummaryEmpty() {
        assertEquals("", runMemory.buildSummary());
    }

    @Test
    void buildSummaryWithEntries() {
        runMemory.put("current_job", "JOB-123");
        runMemory.put("status", "in_progress");
        String summary = runMemory.buildSummary();
        assertTrue(summary.startsWith("## Arbeitsnotizen"));
        assertTrue(summary.contains("current_job: JOB-123"));
        assertTrue(summary.contains("status: in_progress"));
    }

    @Test
    void putOverwritesExisting() {
        runMemory.put("key", "old");
        runMemory.put("key", "new");
        assertEquals("new", runMemory.get("key"));
        assertEquals(1, runMemory.getAll().size());
    }
}
