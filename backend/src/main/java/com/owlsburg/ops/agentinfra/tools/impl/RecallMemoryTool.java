package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryEntity;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryService;
import com.owlsburg.ops.agentinfra.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RecallMemoryTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(RecallMemoryTool.class);

    private final AgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public RecallMemoryTool(AgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "recall_memory";
    }

    @Override
    public String getDescription() {
        return "Ruft Erinnerungen ab. scope=persistent für Langzeitgedächtnis, " +
                "scope=run für Arbeitsnotizen des aktuellen Runs.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "category":{"type":"string","description":"Kategorie zum Filtern (optional, nur persistent)"},
              "limit":{"type":"integer","minimum":1,"maximum":50,"description":"Maximale Anzahl (Standard: 20, nur persistent)"},
              "scope":{"type":"string","enum":["persistent","run"],"default":"persistent","description":"persistent=Langzeitgedächtnis, run=Arbeitsnotizen des aktuellen Runs"}
            }}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String scope = node.has("scope") ? node.get("scope").asText("persistent") : "persistent";

            if ("run".equals(scope)) {
                if (context.runMemory() == null || context.runMemory().isEmpty()) {
                    return ToolResult.success("[]");
                }
                List<Map<String, String>> runEntries = new ArrayList<>();
                for (var entry : context.runMemory().getAll().entrySet()) {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("key", entry.getKey());
                    item.put("value", entry.getValue());
                    runEntries.add(item);
                }
                return ToolResult.success(objectMapper.writeValueAsString(runEntries));
            }

            String category = node.has("category") ? node.get("category").asText() : null;
            int limit = node.has("limit") ? node.get("limit").asInt(20) : 20;

            List<AgentMemoryEntity> memories = memoryService.recallMemories(
                    context.instanceId(), category, limit);

            List<Map<String, Object>> result = new ArrayList<>();
            for (AgentMemoryEntity m : memories) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("type", m.getType());
                item.put("category", m.getCategory());
                item.put("key", m.getKey());
                item.put("value", m.getValue());
                item.put("importance", m.getImportance());
                result.add(item);
            }

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error recalling memory: {}", e.getMessage());
            return ToolResult.error("Fehler beim Abrufen: " + e.getMessage());
        }
    }
}
