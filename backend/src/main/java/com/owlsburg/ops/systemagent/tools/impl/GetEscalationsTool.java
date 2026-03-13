package com.owlsburg.ops.systemagent.tools.impl;

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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class GetEscalationsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetEscalationsTool.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Berlin"));

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public GetEscalationsTool(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_escalations";
    }

    @Override
    public String getDescription() {
        return "Zeigt eskalierte oder dringende Support-Konversationen über alle Tenants. Maximal 20 Ergebnisse.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            // Try with status filter first, fall back to latest 20 if column doesn't exist
            String result = tryQueryWithStatusFilter();
            if (result != null) {
                return SystemToolResult.success(result);
            }

            // Fallback: return latest 20 conversations
            return SystemToolResult.success(queryLatestConversations());
        } catch (Exception e) {
            log.error("Error getting escalations: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen der Eskalationen: " + e.getMessage());
        }
    }

    private String tryQueryWithStatusFilter() {
        String sql = "SELECT t.name as tenant, c.subject, c.status, c.created_at " +
                "FROM conversations c JOIN tenants t ON c.tenant_id = t.id " +
                "WHERE c.status IN ('ESCALATED', 'URGENT') " +
                "ORDER BY c.created_at DESC LIMIT 20";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return formatResults(rs);
        } catch (Exception e) {
            log.debug("Status filter query failed (column may not exist): {}", e.getMessage());
            return null;
        }
    }

    private String queryLatestConversations() throws Exception {
        String sql = "SELECT t.name as tenant, c.subject, c.status, c.created_at " +
                "FROM conversations c JOIN tenants t ON c.tenant_id = t.id " +
                "ORDER BY c.created_at DESC LIMIT 20";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return formatResults(rs);
        }
    }

    private String formatResults(ResultSet rs) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Eskalationen / Neueste Konversationen:\n\n");

        int count = 0;
        while (rs.next()) {
            count++;
            String tenant = rs.getString("tenant");
            String subject = rs.getString("subject");
            String status = rs.getString("status");
            Timestamp createdAt = rs.getTimestamp("created_at");

            String formattedDate = createdAt != null
                    ? FORMATTER.format(createdAt.toInstant())
                    : "–";

            sb.append(count).append(". [").append(tenant).append("] ")
                    .append(subject != null ? subject : "(kein Betreff)")
                    .append(" | Status: ").append(status != null ? status : "–")
                    .append(" | ").append(formattedDate).append("\n");
        }

        if (count == 0) {
            return "Keine Eskalationen.";
        }

        sb.append("\nGesamt: ").append(count).append(" Einträge");
        return sb.toString();
    }
}
