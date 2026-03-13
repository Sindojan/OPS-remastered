package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentRunRepository;
import com.owlsburg.ops.auth.UserRepository;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import com.owlsburg.ops.tenant.TenantEntity;
import com.owlsburg.ops.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Component
public class GetTenantTelemetryTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetTenantTelemetryTool.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AgentRunRepository agentRunRepository;
    private final ObjectMapper objectMapper;

    public GetTenantTelemetryTool(TenantRepository tenantRepository,
                                   UserRepository userRepository,
                                   AgentRunRepository agentRunRepository,
                                   ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.agentRunRepository = agentRunRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_tenant_telemetry";
    }

    @Override
    public String getDescription() {
        return "Gibt detaillierte Telemetrie-Daten für einen Mandanten zurück: Benutzer, Agent-Runs, Token-Verbrauch und Kosten (30 Tage).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "tenant_id":{"type":"string","description":"UUID des Mandanten"}
            },"required":["tenant_id"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String tenantIdStr = node.get("tenant_id").asText();
            UUID tenantId;

            try {
                tenantId = UUID.fromString(tenantIdStr);
            } catch (IllegalArgumentException e) {
                return SystemToolResult.error("Ungültige Mandanten-ID: " + tenantIdStr);
            }

            Optional<TenantEntity> tenantOpt = tenantRepository.findById(tenantId);
            if (tenantOpt.isEmpty()) {
                return SystemToolResult.error("Mandant mit ID '" + tenantIdStr + "' nicht gefunden.");
            }

            TenantEntity tenant = tenantOpt.get();
            Instant since30d = Instant.now().minus(30, ChronoUnit.DAYS);

            long userCount = userRepository.countByTenantId(tenantId);
            long runCount = agentRunRepository.countByTenantSince(tenantId, since30d);
            long totalTokens = agentRunRepository.sumTokensByTenantSince(tenantId, since30d);
            BigDecimal totalCost = agentRunRepository.sumCostByTenantSince(tenantId, since30d);
            Instant lastActive = agentRunRepository.findLastActiveByTenant(tenantId);

            StringBuilder sb = new StringBuilder();
            sb.append("# Telemetrie: ").append(tenant.getName()).append("\n\n");

            sb.append("## Mandanten-Info\n");
            sb.append("| Feld | Wert |\n");
            sb.append("|------|------|\n");
            sb.append("| ID | ").append(tenantId).append(" |\n");
            sb.append("| Name | ").append(tenant.getName()).append(" |\n");
            sb.append("| Slug | ").append(tenant.getSlug() != null ? tenant.getSlug() : "-").append(" |\n");
            sb.append("| Plan | ").append(tenant.getPlan()).append(" |\n");
            sb.append("| Status | ").append(tenant.getStatus()).append(" |\n");
            sb.append("| Aktiv | ").append(tenant.isActive() ? "Ja" : "Nein").append(" |\n");
            sb.append("| Benutzer | ").append(userCount).append(" |\n\n");

            sb.append("## Agent-Nutzung (30 Tage)\n");
            sb.append("| Metrik | Wert |\n");
            sb.append("|--------|------|\n");
            sb.append("| Agent-Runs | ").append(runCount).append(" |\n");
            sb.append("| Tokens gesamt | ").append(String.format("%,d", totalTokens)).append(" |\n");
            sb.append("| Kosten (USD) | $").append(totalCost != null ? totalCost.toPlainString() : "0").append(" |\n");
            sb.append("| Letzte Aktivität | ").append(lastActive != null ? lastActive.toString() : "Keine").append(" |\n");

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Mandanten-Telemetrie: {}", e.getMessage());
            return SystemToolResult.error("Fehler: " + e.getMessage());
        }
    }
}
