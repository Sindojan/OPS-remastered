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
public class AnalyzeCompetitorsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeCompetitorsTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public AnalyzeCompetitorsTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "analyze_competitors";
    }

    @Override
    public String getDescription() {
        return "Analysiert gespeicherte Wettbewerberdaten. Gibt vorhandene Informationen zurück oder empfiehlt eine Web-Suche.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "competitor_name":{"type":"string","description":"Name des Wettbewerbers"}
            },"required":["competitor_name"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String competitorName = node.get("competitor_name").asText();

            if (competitorName.isBlank()) {
                return SystemToolResult.error("Wettbewerbername darf nicht leer sein.");
            }

            List<SystemAgentMemoryEntity> memories = memoryService.recallMemories(
                    context.instanceId(), "competitors", 50);

            List<SystemAgentMemoryEntity> relevant = memories.stream()
                    .filter(m -> m.getKey().toLowerCase().contains(competitorName.toLowerCase()) ||
                                 m.getValue().toLowerCase().contains(competitorName.toLowerCase()))
                    .toList();

            if (relevant.isEmpty()) {
                return SystemToolResult.success(
                        "Keine Daten für '" + competitorName + "' vorhanden. " +
                        "Nutze web_search um Informationen zu sammeln und speichere sie dann mit system_save_memory " +
                        "(Kategorie: 'competitors', Key: '" + competitorName.toLowerCase().replace(" ", "_") + "').");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Wettbewerberanalyse: ").append(competitorName).append("\n\n");

            for (SystemAgentMemoryEntity m : relevant) {
                sb.append("### ").append(m.getKey()).append("\n");
                sb.append(m.getValue()).append("\n\n");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Fehler bei der Wettbewerberanalyse: {}", e.getMessage());
            return SystemToolResult.error("Fehler: " + e.getMessage());
        }
    }
}
