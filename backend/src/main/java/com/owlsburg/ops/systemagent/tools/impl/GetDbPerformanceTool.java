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
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class GetDbPerformanceTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetDbPerformanceTool.class);

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public GetDbPerformanceTool(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_db_performance";
    }

    @Override
    public String getDescription() {
        return "Gibt PostgreSQL Performance-Metriken zurück: DB-Größe, aktive Verbindungen, Connection-States.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setReadOnly(true);
            StringBuilder sb = new StringBuilder();
            sb.append("## Datenbank-Performance\n\n");

            // DB Size
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT pg_database_size(current_database())")) {
                if (rs.next()) {
                    long sizeBytes = rs.getLong(1);
                    double sizeMb = sizeBytes / (1024.0 * 1024.0);
                    sb.append("### Datenbankgröße\n");
                    sb.append("- **Größe:** ").append(String.format("%.2f", sizeMb)).append(" MB\n\n");
                }
            }

            // Active connections
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_stat_activity")) {
                if (rs.next()) {
                    int totalConnections = rs.getInt(1);
                    sb.append("### Verbindungen\n");
                    sb.append("- **Gesamt aktiv:** ").append(totalConnections).append("\n\n");
                }
            }

            // Connection states
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT state, count(*) FROM pg_stat_activity GROUP BY state ORDER BY count(*) DESC")) {
                sb.append("### Verbindungs-Status\n");
                sb.append("| Status | Anzahl |\n");
                sb.append("|--------|--------|\n");
                while (rs.next()) {
                    String state = rs.getString(1);
                    int count = rs.getInt(2);
                    sb.append("| ").append(state != null ? state : "NULL").append(" | ").append(count).append(" |\n");
                }
                sb.append("\n");
            }

            // Table sizes (top 10)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT relname, pg_total_relation_size(relid) AS total_size " +
                                 "FROM pg_catalog.pg_statio_user_tables " +
                                 "ORDER BY pg_total_relation_size(relid) DESC LIMIT 10")) {
                sb.append("### Größte Tabellen (Top 10)\n");
                sb.append("| Tabelle | Größe |\n");
                sb.append("|---------|-------|\n");
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    long tableSize = rs.getLong(2);
                    double tableSizeKb = tableSize / 1024.0;
                    String sizeStr = tableSizeKb >= 1024
                            ? String.format("%.2f MB", tableSizeKb / 1024.0)
                            : String.format("%.1f KB", tableSizeKb);
                    sb.append("| ").append(tableName).append(" | ").append(sizeStr).append(" |\n");
                }
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error getting DB performance: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen der DB-Performance: " + e.getMessage());
        }
    }
}
