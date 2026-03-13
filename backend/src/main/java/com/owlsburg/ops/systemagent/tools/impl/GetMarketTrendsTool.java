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
public class GetMarketTrendsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetMarketTrendsTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public GetMarketTrendsTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_market_trends";
    }

    @Override
    public String getDescription() {
        return "Ruft gespeicherte Markttrends ab, optional gefiltert nach Branche und Region.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "industry":{"type":"string","description":"Branche (z.B. Automotive, Fertigung, SaaS)"},
              "region":{"type":"string","description":"Region (z.B. DACH, Europa, Global) – optional"}
            },"required":["industry"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String industry = node.get("industry").asText();
            String region = node.has("region") ? node.get("region").asText() : null;

            if (industry.isBlank()) {
                return SystemToolResult.error("Branche darf nicht leer sein.");
            }

            List<SystemAgentMemoryEntity> memories = memoryService.recallMemories(
                    context.instanceId(), "market_trends", 50);

            List<SystemAgentMemoryEntity> relevant = memories.stream()
                    .filter(m -> {
                        String combined = (m.getKey() + " " + m.getValue()).toLowerCase();
                        boolean matchesIndustry = combined.contains(industry.toLowerCase());
                        if (region != null && !region.isBlank()) {
                            return matchesIndustry && combined.contains(region.toLowerCase());
                        }
                        return matchesIndustry;
                    })
                    .toList();

            if (relevant.isEmpty()) {
                StringBuilder hint = new StringBuilder();
                hint.append("Keine Markttrends für Branche '").append(industry).append("'");
                if (region != null && !region.isBlank()) {
                    hint.append(" in Region '").append(region).append("'");
                }
                hint.append(" vorhanden.\n\n");
                hint.append("Empfehlung: Nutze web_search mit Suchbegriff '")
                        .append(industry).append(" Markttrends 2026");
                if (region != null && !region.isBlank()) {
                    hint.append(" ").append(region);
                }
                hint.append("' und speichere die Ergebnisse mit system_save_memory (Kategorie: 'market_trends').");

                return SystemToolResult.success(hint.toString());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Markttrends: ").append(industry);
            if (region != null && !region.isBlank()) {
                sb.append(" (").append(region).append(")");
            }
            sb.append("\n\n");

            for (SystemAgentMemoryEntity m : relevant) {
                sb.append("- **").append(m.getKey()).append("**: ").append(m.getValue()).append("\n");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Markttrends: {}", e.getMessage());
            return SystemToolResult.error("Fehler: " + e.getMessage());
        }
    }
}
