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

@Component
public class OptimizePromptsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(OptimizePromptsTool.class);

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public OptimizePromptsTool(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "optimize_prompts";
    }

    @Override
    public String getDescription() {
        return "Analysiert Erfolgsraten und Token-Verbrauch der System-Agents. Basis für Prompt-Optimierung.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "agent_role":{"type":"string","description":"Optional: Rolle des Agents zum Filtern (z.B. 'knowledge_lead')"}
            },"required":[]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String agentRole = node.has("agent_role") && !node.get("agent_role").asText().isBlank()
                    ? node.get("agent_role").asText()
                    : null;

            String sql = "SELECT sai.name, " +
                    "COUNT(*) as total_runs, " +
                    "SUM(CASE WHEN sar.status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count, " +
                    "AVG(sar.tokens_used) as avg_tokens " +
                    "FROM system_agent_runs sar " +
                    "JOIN system_agent_instances sai ON sar.instance_id = sai.id " +
                    (agentRole != null ? "JOIN system_agent_templates sat ON sai.template_id = sat.id WHERE sat.role = ? " : "") +
                    "GROUP BY sai.name";

            StringBuilder sb = new StringBuilder();
            sb.append("Prompt-Optimierungs-Analyse");
            if (agentRole != null) {
                sb.append(" (Rolle: ").append(agentRole).append(")");
            }
            sb.append(":\n\n");

            sb.append(String.format("%-25s | %-10s | %-10s | %-12s | %s%n",
                    "Agent", "Runs", "Erfolge", "Erfolgsrate", "Ø Tokens"));
            sb.append("-".repeat(80)).append("\n");

            int totalRuns = 0;
            int agentCount = 0;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                if (agentRole != null) {
                    ps.setString(1, agentRole);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("name");
                        int runs = rs.getInt("total_runs");
                        int successCount = rs.getInt("success_count");
                        double avgTokens = rs.getDouble("avg_tokens");
                        double successRate = runs > 0 ? (successCount * 100.0 / runs) : 0;

                        sb.append(String.format("%-25s | %-10d | %-10d | %10.1f%% | %.0f%n",
                                name, runs, successCount, successRate, avgTokens));
                        totalRuns += runs;
                        agentCount++;
                    }
                }
            }

            if (agentCount == 0) {
                return SystemToolResult.success("Keine System-Agent-Runs gefunden" +
                        (agentRole != null ? " für Rolle '" + agentRole + "'" : "") + ".");
            }

            sb.append("-".repeat(80)).append("\n");
            sb.append(String.format("Gesamt: %d Runs bei %d Agents%n", totalRuns, agentCount));
            sb.append("\nEmpfehlung: Agents mit niedriger Erfolgsrate oder hohem Token-Verbrauch priorisieren.");

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error analyzing prompts: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Prompt-Analyse: " + e.getMessage());
        }
    }
}
