package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryEntity;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SystemRecallMemoryTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(SystemRecallMemoryTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public SystemRecallMemoryTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "system_recall_memory";
    }

    @Override
    public String getDescription() {
        return "Ruft gespeicherte Erinnerungen ab. Optional nach Kategorie filterbar.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "category":{"type":"string","description":"Kategorie zum Filtern (optional)"},
              "limit":{"type":"integer","minimum":1,"maximum":50,"description":"Max. Anzahl Ergebnisse (optional, default: 10)"}
            },"required":[]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String category = node.has("category") ? node.get("category").asText() : null;
            int limit = node.has("limit") ? node.get("limit").asInt() : 10;

            List<SystemAgentMemoryEntity> memories = memoryService.recallMemories(
                    context.instanceId(), category, limit);

            if (memories.isEmpty()) {
                return SystemToolResult.success("Keine Erinnerungen gefunden" +
                        (category != null ? " für Kategorie '" + category + "'" : "") + ".");
            }

            StringBuilder sb = new StringBuilder();
            for (SystemAgentMemoryEntity m : memories) {
                sb.append("- [").append(m.getCategory()).append("] ").append(m.getKey())
                        .append(" (Wichtigkeit: ").append(m.getImportance()).append("): ")
                        .append(m.getValue()).append("\n");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error recalling system memory: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen: " + e.getMessage());
        }
    }
}
