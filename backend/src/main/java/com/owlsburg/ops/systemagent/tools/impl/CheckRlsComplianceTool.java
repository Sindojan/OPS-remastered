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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class CheckRlsComplianceTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(CheckRlsComplianceTool.class);

    private static final Set<String> EXCLUDED_TABLES = Set.of(
            "flyway_schema_history",
            "system_agent_templates", "system_agent_instances", "system_agent_runs",
            "system_agent_run_steps", "system_agent_memories", "system_agent_messages",
            "system_agent_incidents", "system_chat_sessions", "system_chat_messages",
            "system_api_credentials", "system_llm_config", "system_scheduled_triggers"
    );

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public CheckRlsComplianceTool(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "check_rls_compliance";
    }

    @Override
    public String getDescription() {
        return "Prüft RLS-Compliance: Welche Tabellen haben tenant_id aber kein Row Level Security aktiviert.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setReadOnly(true);

            List<TableInfo> tables = new ArrayList<>();

            // Get all public tables with RLS status
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT schemaname, tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public'")) {
                while (rs.next()) {
                    String tableName = rs.getString("tablename");
                    if (!EXCLUDED_TABLES.contains(tableName)) {
                        tables.add(new TableInfo(
                                tableName,
                                rs.getBoolean("rowsecurity"),
                                false
                        ));
                    }
                }
            }

            // Check which tables have tenant_id column
            for (int i = 0; i < tables.size(); i++) {
                TableInfo table = tables.get(i);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT EXISTS(SELECT 1 FROM information_schema.columns " +
                                     "WHERE table_schema = 'public' AND table_name = '" +
                                     table.name.replace("'", "''") +
                                     "' AND column_name = 'tenant_id')")) {
                    if (rs.next()) {
                        tables.set(i, new TableInfo(table.name, table.rlsEnabled, rs.getBoolean(1)));
                    }
                }
            }

            // Build report
            List<TableInfo> compliant = new ArrayList<>();
            List<TableInfo> nonCompliant = new ArrayList<>();
            List<TableInfo> noTenantId = new ArrayList<>();

            for (TableInfo table : tables) {
                if (!table.hasTenantId) {
                    noTenantId.add(table);
                } else if (table.rlsEnabled) {
                    compliant.add(table);
                } else {
                    nonCompliant.add(table);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("## RLS Compliance Report\n\n");
            sb.append("**Geprüfte Tabellen:** ").append(tables.size()).append("\n");
            sb.append("**Compliant (tenant_id + RLS):** ").append(compliant.size()).append("\n");
            sb.append("**Non-Compliant (tenant_id ohne RLS):** ").append(nonCompliant.size()).append("\n");
            sb.append("**Ohne tenant_id:** ").append(noTenantId.size()).append("\n\n");

            if (!nonCompliant.isEmpty()) {
                sb.append("### Non-Compliant Tabellen\n\n");
                sb.append("| Tabelle | tenant_id | RLS |\n");
                sb.append("|---------|-----------|-----|\n");
                for (TableInfo table : nonCompliant) {
                    sb.append("| ").append(table.name).append(" | Ja | Nein |\n");
                }
                sb.append("\n");
            }

            if (!compliant.isEmpty()) {
                sb.append("### Compliant Tabellen (").append(compliant.size()).append(")\n\n");
                sb.append("| Tabelle | tenant_id | RLS |\n");
                sb.append("|---------|-----------|-----|\n");
                for (TableInfo table : compliant) {
                    sb.append("| ").append(table.name).append(" | Ja | Ja |\n");
                }
                sb.append("\n");
            }

            if (!noTenantId.isEmpty()) {
                sb.append("### Tabellen ohne tenant_id (").append(noTenantId.size()).append(")\n\n");
                for (TableInfo table : noTenantId) {
                    sb.append("- ").append(table.name).append("\n");
                }
            }

            String status = nonCompliant.isEmpty() ? "COMPLIANT" : "NON-COMPLIANT";
            sb.append("\n**Gesamtstatus:** ").append(status);

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error checking RLS compliance: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der RLS-Compliance-Prüfung: " + e.getMessage());
        }
    }

    private record TableInfo(String name, boolean rlsEnabled, boolean hasTenantId) {}
}
