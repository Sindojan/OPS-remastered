package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GenerateContentTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GenerateContentTool.class);

    private final ObjectMapper objectMapper;

    public GenerateContentTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "generate_content";
    }

    @Override
    public String getDescription() {
        return "Erstellt ein strukturiertes Content-Template mit plattformspezifischen Richtlinien. Kein LLM-Aufruf – gibt Vorlage und Richtlinien zurück.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "topic":{"type":"string","description":"Thema des Beitrags"},
              "platform":{"type":"string","description":"Zielplattform (twitter, linkedin, instagram, blog)"},
              "tone":{"type":"string","enum":["professional","casual","creative"],"description":"Tonalität des Beitrags"}
            },"required":["topic","platform","tone"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String topic = node.get("topic").asText();
            String platform = node.get("platform").asText();
            String tone = node.get("tone").asText();

            if (topic.isBlank()) {
                return SystemToolResult.error("Thema darf nicht leer sein.");
            }

            StringBuilder template = new StringBuilder();
            template.append("# Content-Template\n\n");
            template.append("**Thema:** ").append(topic).append("\n");
            template.append("**Plattform:** ").append(platform).append("\n");
            template.append("**Tonalität:** ").append(getToneLabel(tone)).append("\n\n");

            template.append("## Plattform-Richtlinien\n\n");
            template.append(getPlatformGuidelines(platform));

            template.append("\n## Tonalitäts-Hinweise\n\n");
            template.append(getToneGuidelines(tone));

            template.append("\n## Vorgeschlagene Struktur\n\n");
            template.append(getContentStructure(platform));

            template.append("\n---\n");
            template.append("Nutze dieses Template als Basis, um den finalen Beitrag zum Thema '")
                    .append(topic).append("' zu formulieren.");

            return SystemToolResult.success(template.toString());
        } catch (Exception e) {
            log.error("Fehler beim Generieren des Content-Templates: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Generieren: " + e.getMessage());
        }
    }

    private String getToneLabel(String tone) {
        return switch (tone) {
            case "professional" -> "Professionell";
            case "casual" -> "Locker/Informell";
            case "creative" -> "Kreativ";
            default -> tone;
        };
    }

    private String getPlatformGuidelines(String platform) {
        return switch (platform) {
            case "twitter" -> """
                    - Maximale Länge: 280 Zeichen
                    - Hashtags sparsam einsetzen (2-3 max)
                    - Kurze, prägnante Aussagen
                    - Emojis können Engagement erhöhen
                    - Call-to-Action am Ende
                    """;
            case "linkedin" -> """
                    - Optimale Länge: 1.300-2.000 Zeichen
                    - Professioneller Ton bevorzugt
                    - Erste 2-3 Zeilen sind entscheidend (Hook)
                    - Absätze kurz halten (1-2 Sätze)
                    - Hashtags am Ende (3-5 relevant)
                    - Persönliche Erfahrungen kommen gut an
                    """;
            case "instagram" -> """
                    - Caption-Länge: bis 2.200 Zeichen (optimal 125-150 für Feed)
                    - Hashtags: bis 30 erlaubt, 10-15 empfohlen
                    - Visueller Content ist Pflicht (Bild/Video beschreiben)
                    - Erste Zeile muss fesseln
                    - Story-Format für ephemeren Content
                    """;
            case "blog" -> """
                    - Optimale Länge: 1.500-2.500 Wörter
                    - SEO-relevante Überschrift (H1)
                    - Zwischenüberschriften (H2, H3) nutzen
                    - Meta-Description: 150-160 Zeichen
                    - Interne/externe Verlinkungen einbauen
                    - Bilder mit Alt-Text versehen
                    """;
            default -> "- Allgemeine Best Practices: Klar, prägnant, mit Call-to-Action\n";
        };
    }

    private String getToneGuidelines(String tone) {
        return switch (tone) {
            case "professional" -> """
                    - Sachlich und kompetent
                    - Fachbegriffe erlaubt, aber erklärt
                    - Keine Slang-Ausdrücke
                    - Daten und Fakten einbeziehen
                    - Vertrauenswürdig und autoritär
                    """;
            case "casual" -> """
                    - Umgangssprachlich, aber respektvoll
                    - Direkte Ansprache (Du/Ihr)
                    - Humor erlaubt
                    - Kurze Sätze bevorzugt
                    - Emojis sparsam einsetzen
                    """;
            case "creative" -> """
                    - Überraschende Einstiege
                    - Metaphern und Storytelling
                    - Ungewöhnliche Perspektiven
                    - Emotionale Ansprache
                    - Experimentelle Formate erlaubt
                    """;
            default -> "- Ausgewogene, klare Kommunikation\n";
        };
    }

    private String getContentStructure(String platform) {
        return switch (platform) {
            case "twitter" -> """
                    1. Hook (erste Worte fesseln)
                    2. Kernaussage
                    3. CTA oder Hashtags
                    """;
            case "linkedin" -> """
                    1. Hook (1-2 Zeilen)
                    2. Problem/Kontext
                    3. Lösung/Erkenntnis
                    4. Zusammenfassung/Takeaway
                    5. CTA + Hashtags
                    """;
            case "instagram" -> """
                    1. Visuelles Element (Beschreibung)
                    2. Fesselnde erste Zeile
                    3. Story/Inhalt
                    4. CTA
                    5. Hashtags (als Kommentar oder am Ende)
                    """;
            case "blog" -> """
                    1. Überschrift (H1)
                    2. Einleitung mit Hook
                    3. Hauptteil (3-5 Abschnitte mit H2)
                    4. Zusammenfassung
                    5. CTA
                    """;
            default -> """
                    1. Einleitung
                    2. Hauptteil
                    3. Abschluss mit CTA
                    """;
        };
    }
}
