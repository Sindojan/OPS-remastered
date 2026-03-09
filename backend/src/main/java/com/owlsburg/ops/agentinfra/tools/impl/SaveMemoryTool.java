package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryService;
import com.owlsburg.ops.agentinfra.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SaveMemoryTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(SaveMemoryTool.class);

    private final AgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public SaveMemoryTool(AgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "save_memory";
    }

    @Override
    public String getDescription() {
        return "Speichert eine Erinnerung. scope=persistent für Langzeitgedächtnis (DB), " +
                "scope=run für temporäre Arbeitsnotizen (nur aktueller Run).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "type":{"type":"string","enum":["FACT","PREFERENCE","RULE","LEARNING","STRATEGY","WORKFLOW","DECISION","EVENT","NOTE"],"description":"Art der Erinnerung"},
              "category":{"type":"string","description":"Kategorie (z.B. production_patterns, machine_issues)"},
              "key":{"type":"string","description":"Eindeutiger Schlüssel (z.B. cnc3_failure_pattern)"},
              "value":{"type":"string","description":"Inhalt der Erinnerung"},
              "importance":{"type":"integer","minimum":1,"maximum":10,"description":"Wichtigkeit 1-10 (10=kritisch)"},
              "scope":{"type":"string","enum":["persistent","run"],"default":"persistent","description":"persistent=Langzeitgedächtnis, run=nur aktueller Run"}
            },"required":["type","category","key","value","importance"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String type = node.get("type").asText();
            String category = node.get("category").asText();
            String key = node.get("key").asText();
            String value = node.get("value").asText();
            int importance = node.get("importance").asInt(5);
            String scope = node.has("scope") ? node.get("scope").asText("persistent") : "persistent";

            // Input validation
            if (category == null || category.isBlank() || category.length() > 100) {
                return ToolResult.error("category muss 1-100 Zeichen lang sein");
            }
            if (key == null || key.isBlank() || key.length() > 255) {
                return ToolResult.error("key muss 1-255 Zeichen lang sein");
            }
            if (value == null || value.isBlank() || value.length() > 10000) {
                return ToolResult.error("value muss 1-10000 Zeichen lang sein");
            }
            if (importance < 1 || importance > 10) {
                return ToolResult.error("importance muss zwischen 1 und 10 liegen");
            }

            if ("run".equals(scope)) {
                if (context.runMemory() != null) {
                    context.runMemory().put(key, value);
                    return ToolResult.success(objectMapper.writeValueAsString(
                            Map.of("saved", true, "key", key, "scope", "run")));
                }
                return ToolResult.error("RunMemory nicht verfügbar");
            }

            memoryService.saveMemory(context.instanceId(), type, category, key, value, importance);
            return ToolResult.success(objectMapper.writeValueAsString(
                    Map.of("saved", true, "key", key, "scope", "persistent")));
        } catch (Exception e) {
            log.error("Error saving memory: {}", e.getMessage());
            return ToolResult.error("Fehler beim Speichern: " + e.getMessage());
        }
    }
}
