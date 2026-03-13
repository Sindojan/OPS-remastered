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
public class AnalyzeFeedbackTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeFeedbackTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public AnalyzeFeedbackTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "analyze_feedback";
    }

    @Override
    public String getDescription() {
        return "Analysiert gespeichertes Kundenfeedback aus dem Memory für den angegebenen Zeitraum.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "period":{"type":"string","enum":["7d","30d","90d"],"description":"Zeitraum der Analyse"}
            },"required":["period"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String period = node.get("period").asText();

            if (!period.matches("7d|30d|90d")) {
                return SystemToolResult.error("Ungültiger Zeitraum: " + period + ". Erlaubt: 7d, 30d, 90d.");
            }

            List<SystemAgentMemoryEntity> feedback = memoryService.recallMemories(
                    context.instanceId(), "customer_feedback", 50);

            if (feedback.isEmpty()) {
                return SystemToolResult.success(
                        "Kein Kundenfeedback vorhanden für Zeitraum " + period + ".\n\n" +
                        "Hinweis: Feedback kann über system_save_memory mit Kategorie 'customer_feedback' gespeichert werden. " +
                        "Format: Key = 'feedback:{tenant_slug}:{datum}', Value = Feedback-Text.");
            }

            String periodLabel = switch (period) {
                case "7d" -> "7 Tage";
                case "30d" -> "30 Tage";
                case "90d" -> "90 Tage";
                default -> period;
            };

            StringBuilder sb = new StringBuilder();
            sb.append("# Feedback-Analyse (").append(periodLabel).append(")\n\n");
            sb.append("**Anzahl Einträge:** ").append(feedback.size()).append("\n\n");

            sb.append("## Feedback-Einträge\n\n");
            for (SystemAgentMemoryEntity m : feedback) {
                sb.append("- **").append(m.getKey()).append("** (Wichtigkeit: ").append(m.getImportance()).append(")\n");
                sb.append("  ").append(m.getValue()).append("\n\n");
            }

            sb.append("---\n");
            sb.append("*Hinweis: Die Daten stammen aus dem Agent-Memory. Zeitraumfilterung basiert auf den gespeicherten Einträgen.*");

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Fehler bei der Feedback-Analyse: {}", e.getMessage());
            return SystemToolResult.error("Fehler: " + e.getMessage());
        }
    }
}
