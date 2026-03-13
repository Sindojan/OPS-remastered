package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.Set;

@Component
public class GetSupportTrendsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetSupportTrendsTool.class);
    private static final Set<String> VALID_PERIODS = Set.of("7d", "30d", "90d");
    private static final Map<String, String> PERIOD_INTERVALS = Map.of(
            "7d", "7 days",
            "30d", "30 days",
            "90d", "90 days"
    );

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public GetSupportTrendsTool(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_support_trends";
    }

    @Override
    public String getDescription() {
        return "Zeigt Support-Trends (Anzahl Konversationen pro Tenant) für einen Zeitraum. Nützlich zur Erkennung von Support-Hotspots.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "period":{"type":"string","enum":["7d","30d","90d"],"description":"Zeitraum: 7d, 30d oder 90d"}
            },"required":["period"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String period = node.has("period") ? node.get("period").asText() : "30d";

            if (!VALID_PERIODS.contains(period)) {
                return SystemToolResult.error("Ungültiger Zeitraum: " + period + ". Erlaubt: 7d, 30d, 90d");
            }

            String interval = PERIOD_INTERVALS.get(period);

            String sql = "SELECT t.name, COUNT(c.id) as conv_count " +
                    "FROM conversations c JOIN tenants t ON c.tenant_id = t.id " +
                    "WHERE c.created_at > NOW() - INTERVAL '" + interval + "' " +
                    "GROUP BY t.name ORDER BY conv_count DESC";

            StringBuilder sb = new StringBuilder();
            sb.append("Support-Trends (").append(period).append("):\n");
            sb.append(String.format("%-30s | %s%n", "Tenant", "Konversationen"));
            sb.append("-".repeat(45)).append("\n");

            int totalConversations = 0;
            int tenantCount = 0;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String tenantName = rs.getString("name");
                    int convCount = rs.getInt("conv_count");
                    sb.append(String.format("%-30s | %d%n", tenantName, convCount));
                    totalConversations += convCount;
                    tenantCount++;
                }
            }

            if (tenantCount == 0) {
                return SystemToolResult.success("Keine Support-Konversationen im Zeitraum " + period + " gefunden.");
            }

            sb.append("-".repeat(45)).append("\n");
            sb.append(String.format("Gesamt: %d Konversationen bei %d Tenants%n", totalConversations, tenantCount));

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error getting support trends: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen der Support-Trends: " + e.getMessage());
        }
    }
}
