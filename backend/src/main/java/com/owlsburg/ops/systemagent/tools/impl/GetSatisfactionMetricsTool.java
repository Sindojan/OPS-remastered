package com.owlsburg.ops.systemagent.tools.impl;

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
public class GetSatisfactionMetricsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetSatisfactionMetricsTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public GetSatisfactionMetricsTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_satisfaction_metrics";
    }

    @Override
    public String getDescription() {
        return "Ruft gespeicherte Kundenzufriedenheitsdaten ab. Daten werden über system_save_memory in der Kategorie 'satisfaction_data' erfasst.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            List<SystemAgentMemoryEntity> memories = memoryService.recallMemories(
                    context.instanceId(), "satisfaction_data", 50);

            if (memories.isEmpty()) {
                return SystemToolResult.success(
                        "Keine Zufriedenheitsdaten vorhanden. Nutze system_save_memory zum Erfassen.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Kundenzufriedenheits-Metriken:\n\n");

            for (SystemAgentMemoryEntity m : memories) {
                sb.append("- ").append(m.getKey())
                        .append(" (Wichtigkeit: ").append(m.getImportance()).append("): ")
                        .append(m.getValue()).append("\n");
            }

            sb.append("\nGesamt: ").append(memories.size()).append(" Datenpunkte");
            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error getting satisfaction metrics: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen der Zufriedenheitsdaten: " + e.getMessage());
        }
    }
}
