package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentRunRepository;
import com.owlsburg.ops.auth.UserRepository;
import com.owlsburg.ops.common.ModuleService;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class GenerateCustomerReportTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GenerateCustomerReportTool.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AgentRunRepository agentRunRepository;
    private final ModuleService moduleService;
    private final ObjectMapper objectMapper;

    public GenerateCustomerReportTool(TenantRepository tenantRepository,
                                       UserRepository userRepository,
                                       AgentRunRepository agentRunRepository,
                                       ModuleService moduleService,
                                       ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.agentRunRepository = agentRunRepository;
        this.moduleService = moduleService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "generate_customer_report";
    }

    @Override
    public String getDescription() {
        return "Generiert einen Kundenbericht – entweder für einen einzelnen Mandanten (mit Telemetrie) oder eine Gesamtübersicht aller Mandanten.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "tenant_id":{"type":"string","description":"UUID eines Mandanten (optional – ohne ID wird Gesamtübersicht erstellt)"}
            },"required":[]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String tenantIdStr = node.has("tenant_id") && !node.get("tenant_id").isNull()
                    ? node.get("tenant_id").asText() : null;

            if (tenantIdStr != null && !tenantIdStr.isBlank()) {
                return generateSingleTenantReport(tenantIdStr);
            } else {
                return generateOverviewReport();
            }
        } catch (Exception e) {
            log.error("Fehler beim Generieren des Kundenberichts: {}", e.getMessage());
            return SystemToolResult.error("Fehler: " + e.getMessage());
        }
    }

    private SystemToolResult generateSingleTenantReport(String tenantIdStr) {
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
        Set<String> enabledModules = moduleService.getEnabledModules(tenantId);

        StringBuilder report = new StringBuilder();
        report.append("# Kundenbericht: ").append(tenant.getName()).append("\n\n");
        report.append("**Datum:** ").append(LocalDate.now()).append("\n\n");
        report.append("---\n\n");

        report.append("## Mandanten-Profil\n\n");
        report.append("| Feld | Wert |\n");
        report.append("|------|------|\n");
        report.append("| Name | ").append(tenant.getName()).append(" |\n");
        report.append("| Slug | ").append(tenant.getSlug() != null ? tenant.getSlug() : "-").append(" |\n");
        report.append("| Plan | ").append(tenant.getPlan()).append(" |\n");
        report.append("| Status | ").append(tenant.getStatus()).append(" |\n");
        report.append("| Erstellt | ").append(tenant.getCreatedAt() != null ? tenant.getCreatedAt().toString().substring(0, 10) : "-").append(" |\n");
        report.append("| Benutzer | ").append(userCount).append(" |\n\n");

        report.append("## Nutzungsstatistik (30 Tage)\n\n");
        report.append("| Metrik | Wert |\n");
        report.append("|--------|------|\n");
        report.append("| Agent-Runs | ").append(runCount).append(" |\n");
        report.append("| Tokens | ").append(String.format("%,d", totalTokens)).append(" |\n");
        report.append("| Kosten (USD) | $").append(totalCost != null ? totalCost.toPlainString() : "0").append(" |\n");
        report.append("| Letzte Aktivität | ").append(lastActive != null ? lastActive.toString() : "Keine").append(" |\n\n");

        report.append("## Aktive Module\n\n");
        for (String module : enabledModules) {
            report.append("- ").append(module).append("\n");
        }

        report.append("\n## Bewertung\n\n");
        report.append("[Hier eine Bewertung der Kundenbeziehung, Engagement-Level und Empfehlungen einfügen]\n");

        return SystemToolResult.success(report.toString());
    }

    private SystemToolResult generateOverviewReport() {
        List<TenantEntity> tenants = tenantRepository.findAll();

        if (tenants.isEmpty()) {
            return SystemToolResult.success("Keine Mandanten vorhanden – Bericht kann nicht erstellt werden.");
        }

        Instant since30d = Instant.now().minus(30, ChronoUnit.DAYS);

        StringBuilder report = new StringBuilder();
        report.append("# Kunden-Gesamtbericht\n\n");
        report.append("**Datum:** ").append(LocalDate.now()).append("\n");
        report.append("**Mandanten gesamt:** ").append(tenants.size()).append("\n\n");
        report.append("---\n\n");

        report.append("## Übersicht\n\n");
        report.append("| Mandant | Plan | Status | Benutzer | Runs (30d) | Tokens (30d) | Kosten (30d) |\n");
        report.append("|---------|------|--------|----------|------------|-------------|-------------|\n");

        long totalUsers = 0;
        long totalRuns = 0;
        long totalTokens = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (TenantEntity tenant : tenants) {
            long users = userRepository.countByTenantId(tenant.getId());
            long runs = agentRunRepository.countByTenantSince(tenant.getId(), since30d);
            long tokens = agentRunRepository.sumTokensByTenantSince(tenant.getId(), since30d);
            BigDecimal cost = agentRunRepository.sumCostByTenantSince(tenant.getId(), since30d);

            totalUsers += users;
            totalRuns += runs;
            totalTokens += tokens;
            if (cost != null) totalCost = totalCost.add(cost);

            report.append("| ").append(tenant.getName());
            report.append(" | ").append(tenant.getPlan());
            report.append(" | ").append(tenant.getStatus());
            report.append(" | ").append(users);
            report.append(" | ").append(runs);
            report.append(" | ").append(String.format("%,d", tokens));
            report.append(" | $").append(cost != null ? cost.toPlainString() : "0");
            report.append(" |\n");
        }

        report.append("\n**Gesamt:** ").append(totalUsers).append(" Benutzer, ")
                .append(totalRuns).append(" Runs, ")
                .append(String.format("%,d", totalTokens)).append(" Tokens, $")
                .append(totalCost.toPlainString()).append(" Kosten (30d)\n\n");

        report.append("## Empfehlungen\n\n");
        report.append("[Hier strategische Empfehlungen basierend auf den Daten einfügen]\n");

        return SystemToolResult.success(report.toString());
    }
}
