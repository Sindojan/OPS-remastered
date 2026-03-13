package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentRunRepository;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import com.owlsburg.ops.tenant.TenantEntity;
import com.owlsburg.ops.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class AnalyzeChurnRiskTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeChurnRiskTool.class);

    private final TenantRepository tenantRepository;
    private final AgentRunRepository agentRunRepository;
    private final ObjectMapper objectMapper;

    public AnalyzeChurnRiskTool(TenantRepository tenantRepository,
                                 AgentRunRepository agentRunRepository,
                                 ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.agentRunRepository = agentRunRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "analyze_churn_risk";
    }

    @Override
    public String getDescription() {
        return "Analysiert Churn-Risiko aller Tenants basierend auf letzter Aktivität. HIGH = >14 Tage inaktiv, MEDIUM = 7-14 Tage, LOW = <7 Tage.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            List<TenantEntity> tenants = tenantRepository.findByActiveTrue();

            if (tenants.isEmpty()) {
                return SystemToolResult.success("Keine aktiven Tenants gefunden.");
            }

            Instant now = Instant.now();
            List<TenantRisk> risks = new ArrayList<>();

            for (TenantEntity tenant : tenants) {
                Instant lastActive = agentRunRepository.findLastActiveByTenant(tenant.getId());
                String risk;
                long daysInactive;

                if (lastActive == null) {
                    risk = "HIGH";
                    daysInactive = -1; // never active
                } else {
                    daysInactive = ChronoUnit.DAYS.between(lastActive, now);
                    if (daysInactive >= 14) {
                        risk = "HIGH";
                    } else if (daysInactive >= 7) {
                        risk = "MEDIUM";
                    } else {
                        risk = "LOW";
                    }
                }

                risks.add(new TenantRisk(tenant.getName(), risk, daysInactive, lastActive));
            }

            // Sort: HIGH first, then MEDIUM, then LOW
            risks.sort(Comparator.comparingInt(r -> switch (r.risk) {
                case "HIGH" -> 0;
                case "MEDIUM" -> 1;
                default -> 2;
            }));

            StringBuilder sb = new StringBuilder();
            sb.append("Churn-Risiko-Analyse:\n\n");
            sb.append(String.format("%-30s | %-8s | %s%n", "Tenant", "Risiko", "Letzte Aktivität"));
            sb.append("-".repeat(65)).append("\n");

            int highCount = 0, mediumCount = 0, lowCount = 0;

            for (TenantRisk tr : risks) {
                String activityInfo = tr.daysInactive == -1
                        ? "Nie aktiv"
                        : tr.daysInactive + " Tage inaktiv";

                sb.append(String.format("%-30s | %-8s | %s%n", tr.tenantName, tr.risk, activityInfo));

                switch (tr.risk) {
                    case "HIGH" -> highCount++;
                    case "MEDIUM" -> mediumCount++;
                    default -> lowCount++;
                }
            }

            sb.append("-".repeat(65)).append("\n");
            sb.append(String.format("Zusammenfassung: %d HIGH, %d MEDIUM, %d LOW (von %d Tenants)%n",
                    highCount, mediumCount, lowCount, risks.size()));

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error analyzing churn risk: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Churn-Analyse: " + e.getMessage());
        }
    }

    private record TenantRisk(String tenantName, String risk, long daysInactive, Instant lastActive) {}
}
