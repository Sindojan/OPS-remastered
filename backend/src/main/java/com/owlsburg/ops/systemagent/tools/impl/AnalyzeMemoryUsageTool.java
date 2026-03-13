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

@Component
public class AnalyzeMemoryUsageTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeMemoryUsageTool.class);

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public AnalyzeMemoryUsageTool(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "analyze_memory_usage";
    }

    @Override
    public String getDescription() {
        return "Analysiert die Memory-Nutzung der System-Agents: Anzahl Erinnerungen, durchschnittliche Wichtigkeit pro Agent.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            String sql = "SELECT sai.name, COUNT(sam.id) as memory_count, " +
                    "AVG(sam.importance) as avg_importance " +
                    "FROM system_agent_memories sam " +
                    "JOIN system_agent_instances sai ON sam.instance_id = sai.id " +
                    "GROUP BY sai.name";

            StringBuilder sb = new StringBuilder();
            sb.append("System-Agent Memory-Analyse:\n\n");
            sb.append(String.format("%-30s | %-15s | %s%n", "Agent", "Erinnerungen", "Ø Wichtigkeit"));
            sb.append("-".repeat(65)).append("\n");

            int totalMemories = 0;
            int agentCount = 0;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String name = rs.getString("name");
                    int memoryCount = rs.getInt("memory_count");
                    double avgImportance = rs.getDouble("avg_importance");

                    sb.append(String.format("%-30s | %-15d | %.1f%n", name, memoryCount, avgImportance));
                    totalMemories += memoryCount;
                    agentCount++;
                }
            }

            if (agentCount == 0) {
                return SystemToolResult.success("Keine System-Agent-Memories vorhanden.");
            }

            sb.append("-".repeat(65)).append("\n");
            sb.append(String.format("Gesamt: %d Erinnerungen bei %d Agents%n", totalMemories, agentCount));
            sb.append(String.format("Durchschnitt: %.1f Erinnerungen pro Agent%n",
                    (double) totalMemories / agentCount));

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error analyzing memory usage: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Memory-Analyse: " + e.getMessage());
        }
    }
}
