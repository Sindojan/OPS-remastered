package com.owlsburg.ops.systemagent.tools.impl;

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
public class AggregateKnowledgeTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(AggregateKnowledgeTool.class);

    private final DataSource dataSource;
    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public AggregateKnowledgeTool(DataSource dataSource,
                                   SystemAgentMemoryService memoryService,
                                   ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "aggregate_knowledge";
    }

    @Override
    public String getDescription() {
        return "Aggregiert Wissensartikel über alle Tenants und kombiniert mit System-Erinnerungen. Gibt eine Übersicht der Wissensbasis.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Wissens-Aggregation:\n\n");

            // 1. Query knowledge articles across tenants
            sb.append("## Tenant-Wissensartikel\n\n");
            String sql = "SELECT t.name, COUNT(ka.id) as article_count " +
                    "FROM knowledge_articles ka JOIN tenants t ON ka.tenant_id = t.id " +
                    "GROUP BY t.name";

            int totalArticles = 0;
            int tenantCount = 0;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                sb.append(String.format("%-30s | %s%n", "Tenant", "Artikel"));
                sb.append("-".repeat(45)).append("\n");

                while (rs.next()) {
                    String tenantName = rs.getString("name");
                    int articleCount = rs.getInt("article_count");
                    sb.append(String.format("%-30s | %d%n", tenantName, articleCount));
                    totalArticles += articleCount;
                    tenantCount++;
                }
            }

            if (tenantCount == 0) {
                sb.append("Keine Wissensartikel in der Datenbank.\n");
            } else {
                sb.append("-".repeat(45)).append("\n");
                sb.append(String.format("Gesamt: %d Artikel bei %d Tenants%n%n", totalArticles, tenantCount));
            }

            // 2. Recall system memories
            sb.append("## System-Erinnerungen\n\n");
            List<SystemAgentMemoryEntity> memories = memoryService.recallMemories(
                    context.instanceId(), null, 20);

            if (memories.isEmpty()) {
                sb.append("Keine System-Erinnerungen vorhanden.\n");
            } else {
                for (SystemAgentMemoryEntity m : memories) {
                    sb.append("- [").append(m.getCategory()).append("] ").append(m.getKey())
                            .append(": ").append(m.getValue()).append("\n");
                }
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error aggregating knowledge: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Wissens-Aggregation: " + e.getMessage());
        }
    }
}
