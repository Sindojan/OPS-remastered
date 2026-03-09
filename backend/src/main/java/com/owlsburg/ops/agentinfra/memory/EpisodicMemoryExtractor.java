package com.owlsburg.ops.agentinfra.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Deterministic extractor for episodic memories from tool call logs.
 * No LLM involved – pure pattern matching on tool names and results.
 */
@Component
public class EpisodicMemoryExtractor {

    private static final Logger log = LoggerFactory.getLogger(EpisodicMemoryExtractor.class);
    private static final Set<String> STATUS_CHANGE_TOOLS = Set.of(
            "update_job_status", "clock_in", "clock_out", "create_reorder_request"
    );

    private final AgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public EpisodicMemoryExtractor(AgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts episodic memories from a list of tool call records.
     * Each record has: toolName, input (JSON string), result (string), success (boolean).
     */
    public void extractFromToolCalls(UUID instanceId, List<ToolCallRecord> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) return;

        int extracted = 0;
        long timestamp = Instant.now().toEpochMilli();

        for (ToolCallRecord call : toolCalls) {
            try {
                if ("delegate_to_lead".equals(call.toolName())) {
                    String leadName = extractLeadName(call.input());
                    String taskSummary = extractTaskSummary(call.input());
                    String sanitizedLead = sanitizeKey(leadName);
                    String key = "decision_" + sanitizedLead + "_" + timestamp;
                    String value = "Delegation an " + leadName + ": " + taskSummary;
                    memoryService.saveMemory(instanceId, "DECISION", "delegation", key, value, 6, "system");
                    extracted++;
                } else if (isStatusChangeToolCall(call)) {
                    String sanitizedTool = sanitizeKey(call.toolName());
                    String key = "event_" + sanitizedTool + "_" + timestamp;
                    String value = summarizeToolCall(call);
                    memoryService.saveMemory(instanceId, "EVENT", "tool_action", key, value, 4, "system");
                    extracted++;
                }
            } catch (Exception e) {
                log.debug("Failed to extract episodic memory from tool call '{}': {}", call.toolName(), e.getMessage());
            }
        }

        if (extracted > 0) {
            log.debug("Extracted {} episodic memories for instance {}", extracted, instanceId);
        }
    }

    private boolean isStatusChangeToolCall(ToolCallRecord call) {
        if (!call.success()) return false;
        String name = call.toolName();
        return STATUS_CHANGE_TOOLS.contains(name)
                || name.startsWith("create_")
                || name.startsWith("update_");
    }

    private String extractLeadName(String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            if (node.has("lead") && node.get("lead").isTextual()) {
                return node.get("lead").asText();
            }
        } catch (Exception e) {
            log.debug("Failed to parse lead name from input: {}", e.getMessage());
        }
        return "unknown";
    }

    private String extractTaskSummary(String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            if (node.has("task") && node.get("task").isTextual()) {
                String task = node.get("task").asText();
                return task.length() > 100 ? task.substring(0, 100) + "..." : task;
            }
        } catch (Exception e) {
            log.debug("Failed to parse task from input: {}", e.getMessage());
        }
        return "(keine Aufgabe)";
    }

    private String summarizeToolCall(ToolCallRecord call) {
        String result = call.result();
        if (result != null && result.length() > 150) {
            result = result.substring(0, 150) + "...";
        }
        return call.toolName() + " ausgeführt" + (result != null ? ": " + result : "");
    }

    private static String sanitizeKey(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    /**
     * Record of a tool call during an agent run.
     */
    public record ToolCallRecord(String toolName, String input, String result, boolean success) {}
}
