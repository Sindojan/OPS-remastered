package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.AgentActivityStatus;
import com.owlsburg.ops.systemagent.*;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import com.owlsburg.ops.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class GetSystemKpiSummaryTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetSystemKpiSummaryTool.class);

    private final TenantRepository tenantRepository;
    private final SystemAgentInstanceRepository instanceRepository;
    private final SystemAgentRunRepository runRepository;
    private final SystemAgentIncidentRepository incidentRepository;
    private final ObjectMapper objectMapper;

    public GetSystemKpiSummaryTool(TenantRepository tenantRepository,
                                    SystemAgentInstanceRepository instanceRepository,
                                    SystemAgentRunRepository runRepository,
                                    SystemAgentIncidentRepository incidentRepository,
                                    ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.instanceRepository = instanceRepository;
        this.runRepository = runRepository;
        this.incidentRepository = incidentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_system_kpi_summary";
    }

    @Override
    public String getDescription() {
        return "Gibt eine Übersicht der wichtigsten System-KPIs: Tenants, System-Agent-Aktivität, Token-Verbrauch, Fehler.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

            long totalTenants = tenantRepository.count();
            long totalSystemAgents = instanceRepository.count();
            long busyAgents = instanceRepository.findByActivityStatus(AgentActivityStatus.BUSY).size();
            long errorAgents = instanceRepository.findByActivityStatus(AgentActivityStatus.ERROR).size();
            long totalRuns30d = runRepository.countSince(thirtyDaysAgo);
            long failedRuns30d = runRepository.countFailedSince(thirtyDaysAgo);
            long totalTokens30d = runRepository.sumTokensSince(thirtyDaysAgo);
            BigDecimal totalCost30d = runRepository.sumCostSince(thirtyDaysAgo);
            long openIncidents = incidentRepository.countByResolvedAtIsNull();

            ObjectNode result = objectMapper.createObjectNode();
            result.put("totalTenants", totalTenants);
            result.put("totalSystemAgents", totalSystemAgents);
            result.put("busyAgents", busyAgents);
            result.put("errorAgents", errorAgents);
            result.put("totalRuns30d", totalRuns30d);
            result.put("failedRuns30d", failedRuns30d);
            result.put("totalTokens30d", totalTokens30d);
            result.put("totalCostUsd30d", totalCost30d);
            result.put("openIncidents", openIncidents);

            return SystemToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error getting system KPI summary: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen der System-KPIs: " + e.getMessage());
        }
    }
}
