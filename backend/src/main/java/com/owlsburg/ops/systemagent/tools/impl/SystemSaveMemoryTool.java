package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SystemSaveMemoryTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(SystemSaveMemoryTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public SystemSaveMemoryTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "system_save_memory";
    }

    @Override
    public String getDescription() {
        return "Speichert eine Erinnerung im System-Agent-Memory. Nutze dies für wichtige Fakten, Erkenntnisse oder Entscheidungen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "category":{"type":"string","description":"Kategorie (z.B. 'market_data', 'tenant_info', 'security')"},
              "key":{"type":"string","description":"Eindeutiger Schlüssel"},
              "value":{"type":"string","description":"Zu speichernder Inhalt"},
              "type":{"type":"string","enum":["SEMANTIC","PROCEDURAL","EPISODIC"],"description":"Memory-Typ (optional, default: SEMANTIC)"},
              "importance":{"type":"integer","minimum":1,"maximum":10,"description":"Wichtigkeit 1-10 (optional, default: 5)"}
            },"required":["category","key","value"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String category = node.get("category").asText();
            String key = node.get("key").asText();
            String value = node.get("value").asText();
            String type = node.has("type") ? node.get("type").asText() : "SEMANTIC";
            int importance = node.has("importance") ? node.get("importance").asInt() : 5;

            memoryService.saveMemory(context.instanceId(), type, category, key, value, importance);
            return SystemToolResult.success("Erinnerung gespeichert: [" + category + "] " + key);
        } catch (Exception e) {
            log.error("Error saving system memory: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Speichern: " + e.getMessage());
        }
    }
}
