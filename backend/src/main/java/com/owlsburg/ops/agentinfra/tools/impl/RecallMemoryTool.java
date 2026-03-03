package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryEntity;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryService;
import com.owlsburg.ops.agentinfra.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

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
        return "Ruft gespeicherte Erinnerungen aus dem Langzeitgedächtnis ab. " +
                "Optional nach Kategorie filterbar.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "category":{"type":"string","description":"Kategorie zum Filtern (optional)"},
              "limit":{"type":"integer","minimum":1,"maximum":50,"description":"Maximale Anzahl (Standard: 20)"}
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
            String category = node.has("category") ? node.get("category").asText() : null;
            int limit = node.has("limit") ? node.get("limit").asInt(20) : 20;

            List<AgentMemoryEntity> memories = memoryService.recallMemories(
                    context.instanceId(), category, limit);

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < memories.size(); i++) {
                AgentMemoryEntity m = memories.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"type\":\"").append(m.getType())
                  .append("\",\"category\":\"").append(m.getCategory())
                  .append("\",\"key\":\"").append(m.getKey())
                  .append("\",\"value\":").append(objectMapper.writeValueAsString(m.getValue()))
                  .append(",\"importance\":").append(m.getImportance())
                  .append("}");
            }
            sb.append("]");

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error recalling memory: {}", e.getMessage());
            return ToolResult.error("Fehler beim Abrufen: " + e.getMessage());
        }
    }
}
