package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryService;
import com.owlsburg.ops.agentinfra.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
        return "Speichert eine Erinnerung im Langzeitgedächtnis. " +
                "Nutze dies für wichtige Fakten, Muster oder Erkenntnisse die du dir merken möchtest.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "type":{"type":"string","enum":["FACT","PREFERENCE","LEARNING","NOTE"],"description":"Art der Erinnerung"},
              "category":{"type":"string","description":"Kategorie (z.B. production_patterns, machine_issues)"},
              "key":{"type":"string","description":"Eindeutiger Schlüssel (z.B. cnc3_failure_pattern)"},
              "value":{"type":"string","description":"Inhalt der Erinnerung"},
              "importance":{"type":"integer","minimum":1,"maximum":10,"description":"Wichtigkeit 1-10 (10=kritisch)"}
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

            memoryService.saveMemory(context.instanceId(), type, category, key, value, importance);

            return ToolResult.success("{\"saved\":true,\"key\":\"" + key + "\"}");
        } catch (Exception e) {
            log.error("Error saving memory: {}", e.getMessage());
            return ToolResult.error("Fehler beim Speichern: " + e.getMessage());
        }
    }
}
