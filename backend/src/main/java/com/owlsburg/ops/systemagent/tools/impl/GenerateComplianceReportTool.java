package com.owlsburg.ops.systemagent.tools.impl;

import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GenerateComplianceReportTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GenerateComplianceReportTool.class);

    private final SystemAgentMemoryService memoryService;

    public GenerateComplianceReportTool(SystemAgentMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String getName() {
        return "generate_compliance_report";
    }

    @Override
    public String getDescription() {
        return "Erstellt einen Compliance-Report basierend auf RLS-Status, Security-Logs und Auth-Anomalien aus dem System-Memory.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            var securityMemories = memoryService.recallMemories(context.instanceId(), "security", 20);
            var rlsMemories = memoryService.recallMemories(context.instanceId(), "rls", 10);
            var authMemories = memoryService.recallMemories(context.instanceId(), "auth", 10);

            StringBuilder report = new StringBuilder();
            report.append("# Compliance Report\n\n");

            report.append("## RLS-Status (").append(rlsMemories.size()).append(" Einträge)\n");
            for (var m : rlsMemories) {
                report.append("- [").append(m.getKey()).append("] ").append(m.getValue()).append("\n");
            }

            report.append("\n## Security-Findings (").append(securityMemories.size()).append(" Einträge)\n");
            for (var m : securityMemories) {
                report.append("- [").append(m.getKey()).append("] ").append(m.getValue()).append("\n");
            }

            report.append("\n## Auth-Anomalien (").append(authMemories.size()).append(" Einträge)\n");
            for (var m : authMemories) {
                report.append("- [").append(m.getKey()).append("] ").append(m.getValue()).append("\n");
            }

            if (securityMemories.isEmpty() && rlsMemories.isEmpty() && authMemories.isEmpty()) {
                report.append("\nKeine relevanten Daten im Memory vorhanden. ")
                        .append("Bitte zuerst scan_security_logs, check_rls_compliance und get_auth_anomalies ausführen.");
            }

            return SystemToolResult.success(report.toString());
        } catch (Exception e) {
            log.error("Error generating compliance report: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Erstellen des Compliance-Reports: " + e.getMessage());
        }
    }
}
