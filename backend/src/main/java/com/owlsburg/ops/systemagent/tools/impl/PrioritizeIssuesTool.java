package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PrioritizeIssuesTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(PrioritizeIssuesTool.class);

    private final ObjectMapper objectMapper;

    public PrioritizeIssuesTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "prioritize_issues";
    }

    @Override
    public String getDescription() {
        return "Erstellt eine strukturierte Priorisierungsvorlage für eine Liste von Issues. Hilft bei der systematischen Bewertung nach Impact, Häufigkeit und Dringlichkeit.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "issues":{"type":"string","description":"Komma-separierte Liste der zu priorisierenden Issues"}
            },"required":["issues"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);

            if (!node.has("issues") || node.get("issues").asText().isBlank()) {
                return SystemToolResult.error("Parameter 'issues' ist erforderlich und darf nicht leer sein.");
            }

            String issuesRaw = node.get("issues").asText();
            List<String> issues = Arrays.stream(issuesRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            if (issues.isEmpty()) {
                return SystemToolResult.error("Keine gültigen Issues in der Liste gefunden.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Issue-Priorisierung (").append(issues.size()).append(" Issues):\n\n");
            sb.append(String.format("%-4s | %-40s | %-10s | %-12s | %-12s | %s%n",
                    "Nr.", "Issue", "Impact", "Häufigkeit", "Dringlichkeit", "Priorität"));
            sb.append("-".repeat(100)).append("\n");

            for (int i = 0; i < issues.size(); i++) {
                sb.append(String.format("%-4d | %-40s | %-10s | %-12s | %-12s | %s%n",
                        i + 1,
                        truncate(issues.get(i), 40),
                        "[bewerten]",
                        "[bewerten]",
                        "[bewerten]",
                        "[berechnen]"));
            }

            sb.append("-".repeat(100)).append("\n");
            sb.append("\nBewertungsskala: HOCH / MITTEL / NIEDRIG\n");
            sb.append("Priorität = Impact × Häufigkeit × Dringlichkeit\n");
            sb.append("\nEmpfehlung: Analysiere jeden Issue anhand der Kriterien und fülle die Tabelle aus.\n");
            sb.append("Nutze die verfügbaren Daten (Support-Trends, Eskalationen, Churn-Risiko) zur Bewertung.");

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error prioritizing issues: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Issue-Priorisierung: " + e.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
