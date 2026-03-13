package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.auth.UserRepository;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import com.owlsburg.ops.tenant.TenantEntity;
import com.owlsburg.ops.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetTenantOverviewTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetTenantOverviewTool.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public GetTenantOverviewTool(TenantRepository tenantRepository,
                                  UserRepository userRepository,
                                  ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_tenant_overview";
    }

    @Override
    public String getDescription() {
        return "Gibt eine Übersicht aller Mandanten mit Benutzerzahlen, Plan und Status zurück.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{},"required":[]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            List<TenantEntity> tenants = tenantRepository.findAll();

            if (tenants.isEmpty()) {
                return SystemToolResult.success("Keine Mandanten vorhanden.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("# Mandanten-Übersicht\n\n");
            sb.append("| Name | Slug | Plan | Status | Aktiv | Benutzer | Erstellt |\n");
            sb.append("|------|------|------|--------|-------|----------|----------|\n");

            long totalUsers = 0;
            int activeTenants = 0;

            for (TenantEntity tenant : tenants) {
                long userCount = userRepository.countByTenantId(tenant.getId());
                totalUsers += userCount;
                if (tenant.isActive()) activeTenants++;

                sb.append("| ").append(tenant.getName());
                sb.append(" | ").append(tenant.getSlug() != null ? tenant.getSlug() : "-");
                sb.append(" | ").append(tenant.getPlan());
                sb.append(" | ").append(tenant.getStatus());
                sb.append(" | ").append(tenant.isActive() ? "Ja" : "Nein");
                sb.append(" | ").append(userCount);
                sb.append(" | ").append(tenant.getCreatedAt() != null ? tenant.getCreatedAt().toString().substring(0, 10) : "-");
                sb.append(" |\n");
            }

            sb.append("\n**Gesamt:** ").append(tenants.size()).append(" Mandanten (")
                    .append(activeTenants).append(" aktiv), ").append(totalUsers).append(" Benutzer");

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Mandanten-Übersicht: {}", e.getMessage());
            return SystemToolResult.error("Fehler: " + e.getMessage());
        }
    }
}
