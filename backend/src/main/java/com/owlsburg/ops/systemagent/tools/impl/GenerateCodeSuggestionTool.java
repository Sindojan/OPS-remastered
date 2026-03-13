package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GenerateCodeSuggestionTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GenerateCodeSuggestionTool.class);
    private static final Set<String> ALLOWED_LANGUAGES = Set.of("java", "typescript", "sql");

    private final ObjectMapper objectMapper;

    public GenerateCodeSuggestionTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "generate_code_suggestion";
    }

    @Override
    public String getDescription() {
        return "Erstellt ein strukturiertes Code-Suggestion-Template basierend auf Beschreibung, Sprache und Kontext.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "description":{"type":"string","description":"Beschreibung der gewünschten Code-Änderung"},
              "language":{"type":"string","enum":["java","typescript","sql"],"description":"Zielsprache"},
              "context":{"type":"string","description":"Zusätzlicher Kontext (optional)"}
            },"required":["description","language"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);

            String description = node.has("description") ? node.get("description").asText() : null;
            String language = node.has("language") ? node.get("language").asText() : null;
            String codeContext = node.has("context") ? node.get("context").asText() : null;

            if (description == null || description.isBlank()) {
                return SystemToolResult.error("Beschreibung ist erforderlich.");
            }
            if (language == null || !ALLOWED_LANGUAGES.contains(language)) {
                return SystemToolResult.error("Ungültige Sprache. Erlaubt: " + ALLOWED_LANGUAGES);
            }

            StringBuilder template = new StringBuilder();
            template.append("## Code Suggestion\n\n");
            template.append("**Sprache:** ").append(language).append("\n");
            template.append("**Beschreibung:** ").append(description).append("\n\n");

            template.append("### Projekt-Constraints\n\n");
            switch (language) {
                case "java" -> {
                    template.append("- Spring Boot 3.5.0, Java 21\n");
                    template.append("- Kein Lombok (@Slf4j, @RequiredArgsConstructor) – explizite Logger + Konstruktoren\n");
                    template.append("- Controller -> Service -> Repository (nie überspringen)\n");
                    template.append("- Alle Entities mit tenant_id + RLS\n");
                    template.append("- @Component/@Service Annotationen für DI\n");
                }
                case "typescript" -> {
                    template.append("- Next.js 16, React 19, TypeScript\n");
                    template.append("- shadcn/ui + Tailwind CSS v4 only\n");
                    template.append("- Keine eigenen CSS-Klassen\n");
                    template.append("- Pfad-Aliases: @/components, @/lib, @/types\n");
                    template.append("- UI komplett auf Deutsch\n");
                }
                case "sql" -> {
                    template.append("- PostgreSQL, Flyway-Migrationen\n");
                    template.append("- Single-Schema mit Row Level Security\n");
                    template.append("- Jede Tabelle braucht tenant_id + RLS Policy\n");
                    template.append("- Flache Migrationsstruktur (V1, V2, ...)\n");
                }
            }

            if (codeContext != null && !codeContext.isBlank()) {
                template.append("\n### Kontext\n\n").append(codeContext).append("\n");
            }

            template.append("\n### Aufgabe\n\n");
            template.append("Erstelle einen Code-Vorschlag für: ").append(description).append("\n");

            return SystemToolResult.success(template.toString());
        } catch (Exception e) {
            log.error("Error generating code suggestion: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Erstellen des Code-Vorschlags: " + e.getMessage());
        }
    }
}
