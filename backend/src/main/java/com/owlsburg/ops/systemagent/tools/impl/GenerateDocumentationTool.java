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
public class GenerateDocumentationTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GenerateDocumentationTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public GenerateDocumentationTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "generate_documentation";
    }

    @Override
    public String getDescription() {
        return "Generiert eine Dokumentationsvorlage zu einem Thema, angereichert mit relevanten System-Erinnerungen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "topic":{"type":"string","description":"Thema für die Dokumentation"}
            },"required":["topic"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);

            if (!node.has("topic") || node.get("topic").asText().isBlank()) {
                return SystemToolResult.error("Parameter 'topic' ist erforderlich und darf nicht leer sein.");
            }

            String topic = node.get("topic").asText();

            // Search across multiple categories for relevant memories
            List<SystemAgentMemoryEntity> allMemories = memoryService.recallMemories(
                    context.instanceId(), null, 50);

            // Filter memories that are related to the topic (case-insensitive search in key and value)
            String topicLower = topic.toLowerCase();
            List<SystemAgentMemoryEntity> relevantMemories = allMemories.stream()
                    .filter(m -> m.getKey().toLowerCase().contains(topicLower)
                            || m.getValue().toLowerCase().contains(topicLower)
                            || m.getCategory().toLowerCase().contains(topicLower))
                    .toList();

            StringBuilder sb = new StringBuilder();
            sb.append("# Dokumentation: ").append(topic).append("\n\n");

            sb.append("## 1. Übersicht\n");
            sb.append("[Zusammenfassung des Themas hier einfügen]\n\n");

            sb.append("## 2. Kontext & Hintergrund\n");
            sb.append("[Hintergrund-Informationen hier einfügen]\n\n");

            sb.append("## 3. Details\n");
            sb.append("[Detaillierte Beschreibung hier einfügen]\n\n");

            sb.append("## 4. Empfehlungen\n");
            sb.append("[Handlungsempfehlungen hier einfügen]\n\n");

            // Add gathered context
            sb.append("## Gesammelter Kontext\n\n");

            if (relevantMemories.isEmpty()) {
                sb.append("Keine relevanten Erinnerungen zum Thema '").append(topic).append("' gefunden.\n");
                sb.append("Empfehlung: Sammle zuerst Daten über die verfügbaren Tools und speichere Erkenntnisse mit system_save_memory.\n");
            } else {
                sb.append("Relevante Erinnerungen (").append(relevantMemories.size()).append("):\n\n");
                for (SystemAgentMemoryEntity m : relevantMemories) {
                    sb.append("- **[").append(m.getCategory()).append("] ").append(m.getKey()).append("**\n");
                    sb.append("  ").append(m.getValue()).append("\n");
                    sb.append("  (Typ: ").append(m.getType())
                            .append(", Wichtigkeit: ").append(m.getImportance()).append(")\n\n");
                }
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error generating documentation: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Dokumentations-Generierung: " + e.getMessage());
        }
    }
}
