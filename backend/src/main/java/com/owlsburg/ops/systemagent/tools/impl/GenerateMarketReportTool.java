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

import java.time.LocalDate;
import java.util.List;

@Component
public class GenerateMarketReportTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GenerateMarketReportTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public GenerateMarketReportTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "generate_market_report";
    }

    @Override
    public String getDescription() {
        return "Generiert ein strukturiertes Marktbericht-Template mit gesammelten Daten aus dem Memory (Trends, Wettbewerber).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "topic":{"type":"string","description":"Thema/Titel des Marktberichts"},
              "include_competitors":{"type":"boolean","description":"Wettbewerberdaten einbeziehen (optional, default: false)"}
            },"required":["topic"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String topic = node.get("topic").asText();
            boolean includeCompetitors = node.has("include_competitors") && node.get("include_competitors").asBoolean();

            if (topic.isBlank()) {
                return SystemToolResult.error("Thema darf nicht leer sein.");
            }

            List<SystemAgentMemoryEntity> trends = memoryService.recallMemories(
                    context.instanceId(), "market_trends", 20);
            List<SystemAgentMemoryEntity> competitors = includeCompetitors
                    ? memoryService.recallMemories(context.instanceId(), "competitors", 20)
                    : List.of();

            StringBuilder report = new StringBuilder();
            report.append("# Marktbericht: ").append(topic).append("\n\n");
            report.append("**Datum:** ").append(LocalDate.now()).append("\n\n");
            report.append("---\n\n");

            report.append("## 1. Zusammenfassung\n\n");
            report.append("[Hier eine Executive Summary zum Thema '").append(topic).append("' einfügen]\n\n");

            report.append("## 2. Marktüberblick\n\n");
            report.append("[Allgemeine Marktlage, Volumen, Wachstumsraten]\n\n");

            report.append("## 3. Trends & Entwicklungen\n\n");
            if (trends.isEmpty()) {
                report.append("*Keine Trenddaten im Memory vorhanden. Nutze web_search und get_market_trends um Daten zu sammeln.*\n\n");
            } else {
                for (SystemAgentMemoryEntity m : trends) {
                    report.append("- **").append(m.getKey()).append("**: ").append(m.getValue()).append("\n");
                }
                report.append("\n");
            }

            if (includeCompetitors) {
                report.append("## 4. Wettbewerbsanalyse\n\n");
                if (competitors.isEmpty()) {
                    report.append("*Keine Wettbewerberdaten im Memory vorhanden. Nutze analyze_competitors und web_search um Daten zu sammeln.*\n\n");
                } else {
                    for (SystemAgentMemoryEntity m : competitors) {
                        report.append("### ").append(m.getKey()).append("\n");
                        report.append(m.getValue()).append("\n\n");
                    }
                }

                report.append("## 5. Chancen & Risiken\n\n");
            } else {
                report.append("## 4. Chancen & Risiken\n\n");
            }
            report.append("[SWOT-Analyse oder Chancen/Risiken-Bewertung hier einfügen]\n\n");

            int nextSection = includeCompetitors ? 6 : 5;
            report.append("## ").append(nextSection).append(". Handlungsempfehlungen\n\n");
            report.append("[Konkrete Empfehlungen basierend auf den gesammelten Daten]\n\n");

            report.append("---\n\n");
            report.append("*Datenquellen: ").append(trends.size()).append(" Trend-Einträge");
            if (includeCompetitors) {
                report.append(", ").append(competitors.size()).append(" Wettbewerber-Einträge");
            }
            report.append(" aus dem Agent-Memory.*\n");

            return SystemToolResult.success(report.toString());
        } catch (Exception e) {
            log.error("Fehler beim Generieren des Marktberichts: {}", e.getMessage());
            return SystemToolResult.error("Fehler: " + e.getMessage());
        }
    }
}
