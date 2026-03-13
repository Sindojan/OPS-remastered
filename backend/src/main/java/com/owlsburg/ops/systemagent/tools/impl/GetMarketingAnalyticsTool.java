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
public class GetMarketingAnalyticsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetMarketingAnalyticsTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public GetMarketingAnalyticsTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_marketing_analytics";
    }

    @Override
    public String getDescription() {
        return "Ruft Marketing-Analytics für eine Plattform und einen Zeitraum ab. Im MVP-Modus werden gespeicherte Daten aus dem Memory zurückgegeben.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "platform":{"type":"string","description":"Plattform (z.B. twitter, linkedin, instagram)"},
              "period":{"type":"string","enum":["7d","30d","90d"],"description":"Zeitraum für die Analyse"}
            },"required":["platform","period"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String platform = node.get("platform").asText();
            String period = node.get("period").asText();

            if (!period.matches("7d|30d|90d")) {
                return SystemToolResult.error("Ungültiger Zeitraum: " + period + ". Erlaubt: 7d, 30d, 90d.");
            }

            log.info("Marketing-Analytics angefragt für Plattform '{}', Zeitraum '{}' – echte API-Integration steht noch aus", platform, period);

            List<SystemAgentMemoryEntity> memories = memoryService.recallMemories(
                    context.instanceId(), "marketing_analytics", 20);

            if (memories.isEmpty()) {
                return SystemToolResult.success(
                        "Keine Marketing-Analytics-Daten verfügbar für " + platform + " (" + period + ").\n\n" +
                        "Hinweis: Die echte API-Integration ist noch nicht implementiert. " +
                        "Gespeicherte Analytics-Daten können über system_save_memory mit Kategorie 'marketing_analytics' hinterlegt werden.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Marketing-Analytics (").append(platform).append(", ").append(period).append("):\n\n");

            for (SystemAgentMemoryEntity m : memories) {
                if (m.getKey().contains(platform) || !m.getKey().contains(":")) {
                    sb.append("- ").append(m.getKey()).append(": ").append(m.getValue()).append("\n");
                }
            }

            sb.append("\nHinweis: Daten stammen aus dem Memory (MVP-Modus). Echte API-Integration steht noch aus.");

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Marketing-Analytics: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen: " + e.getMessage());
        }
    }
}
