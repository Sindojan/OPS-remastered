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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

@Component
public class SearchKnowledgeBaseTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(SearchKnowledgeBaseTool.class);
    private static final int MAX_RESULTS = 20;

    private final SystemAgentMemoryService memoryService;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public SearchKnowledgeBaseTool(SystemAgentMemoryService memoryService,
                                    DataSource dataSource,
                                    ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "search_knowledge_base";
    }

    @Override
    public String getDescription() {
        return "Durchsucht die Wissensbasis. Scope 'system' = System-Erinnerungen, Scope 'all' = zusätzlich Tenant-Wissensartikel.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "query":{"type":"string","description":"Suchbegriff"},
              "scope":{"type":"string","enum":["system","all"],"description":"Suchbereich: 'system' (default) oder 'all' (inkl. Tenant-Artikel)"}
            },"required":["query"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);

            if (!node.has("query") || node.get("query").asText().isBlank()) {
                return SystemToolResult.error("Parameter 'query' ist erforderlich und darf nicht leer sein.");
            }

            String query = node.get("query").asText();
            String scope = node.has("scope") ? node.get("scope").asText() : "system";

            StringBuilder sb = new StringBuilder();
            sb.append("Suchergebnisse für '").append(query).append("'");
            sb.append(" (Scope: ").append(scope).append("):\n\n");

            int totalResults = 0;

            // 1. Always search system memories
            sb.append("## System-Erinnerungen\n\n");
            List<SystemAgentMemoryEntity> memories = memoryService.recallMemories(
                    context.instanceId(), null, 50);

            String queryLower = query.toLowerCase();
            List<SystemAgentMemoryEntity> matchingMemories = memories.stream()
                    .filter(m -> m.getKey().toLowerCase().contains(queryLower)
                            || m.getValue().toLowerCase().contains(queryLower))
                    .limit(MAX_RESULTS)
                    .toList();

            if (matchingMemories.isEmpty()) {
                sb.append("Keine passenden System-Erinnerungen gefunden.\n\n");
            } else {
                for (SystemAgentMemoryEntity m : matchingMemories) {
                    sb.append("- [").append(m.getCategory()).append("] **").append(m.getKey()).append("**: ")
                            .append(m.getValue()).append("\n");
                    totalResults++;
                }
                sb.append("\n");
            }

            // 2. If scope=all, also search tenant knowledge articles
            if ("all".equals(scope)) {
                sb.append("## Tenant-Wissensartikel\n\n");
                int remaining = MAX_RESULTS - totalResults;

                if (remaining > 0) {
                    String sql = "SELECT t.name as tenant, ka.title, ka.excerpt " +
                            "FROM knowledge_articles ka " +
                            "JOIN tenants t ON ka.tenant_id = t.id " +
                            "WHERE LOWER(ka.title) LIKE ? OR LOWER(ka.excerpt) LIKE ? " +
                            "ORDER BY ka.created_at DESC LIMIT ?";

                    String likePattern = "%" + queryLower + "%";

                    try (Connection conn = dataSource.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {

                        ps.setString(1, likePattern);
                        ps.setString(2, likePattern);
                        ps.setInt(3, remaining);

                        try (ResultSet rs = ps.executeQuery()) {
                            boolean hasResults = false;
                            while (rs.next()) {
                                hasResults = true;
                                String tenant = rs.getString("tenant");
                                String title = rs.getString("title");
                                String excerpt = rs.getString("excerpt");

                                sb.append("- [").append(tenant).append("] **").append(title).append("**");
                                if (excerpt != null && !excerpt.isBlank()) {
                                    sb.append(": ").append(truncate(excerpt, 100));
                                }
                                sb.append("\n");
                                totalResults++;
                            }

                            if (!hasResults) {
                                sb.append("Keine passenden Tenant-Artikel gefunden.\n");
                            }
                        }
                    }
                    sb.append("\n");
                }
            }

            sb.append("Gesamt: ").append(totalResults).append(" Ergebnisse");
            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error searching knowledge base: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Wissenssuche: " + e.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
