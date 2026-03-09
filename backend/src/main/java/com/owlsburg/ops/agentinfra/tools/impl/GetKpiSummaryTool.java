package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.common.ModuleService;
import com.owlsburg.ops.inbox.ConversationEntity;
import com.owlsburg.ops.inbox.ConversationService;
import com.owlsburg.ops.inventory.StockService;
import com.owlsburg.ops.inventory.dto.CriticalArticleResponse;
import com.owlsburg.ops.machines.MachineService;
import com.owlsburg.ops.machines.dto.MachineResponse;
import com.owlsburg.ops.people.EmployeeEntity;
import com.owlsburg.ops.people.EmployeeService;
import com.owlsburg.ops.production.JobService;
import com.owlsburg.ops.production.JobStatus;
import com.owlsburg.ops.production.dto.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetKpiSummaryTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetKpiSummaryTool.class);

    private final JobService jobService;
    private final StockService stockService;
    private final MachineService machineService;
    private final EmployeeService employeeService;
    private final ConversationService conversationService;
    private final ModuleService moduleService;
    private final ObjectMapper objectMapper;

    public GetKpiSummaryTool(JobService jobService,
                              StockService stockService,
                              MachineService machineService,
                              EmployeeService employeeService,
                              ConversationService conversationService,
                              ModuleService moduleService,
                              ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.stockService = stockService;
        this.machineService = machineService;
        this.employeeService = employeeService;
        this.conversationService = conversationService;
        this.moduleService = moduleService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_kpi_summary";
    }

    @Override
    public String getDescription() {
        return "Gibt eine aggregierte KPI-Übersicht zurück: Aufträge nach Status, kritischer Bestand, Maschinenstatus, Mitarbeiterzahl und offene Konversationen.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            UUID tenantId = UUID.fromString(context.tenantId());
            Set<String> modules = moduleService.getEnabledModules(tenantId);
            Map<String, Object> result = new LinkedHashMap<>();

            // Jobs by status (production module)
            if (modules.contains("production")) {
                Page<JobResponse> allJobs = jobService.getAll(Pageable.unpaged());
                Map<String, Long> jobsByStatus = allJobs.getContent().stream()
                        .collect(Collectors.groupingBy(
                                job -> job.status().name(),
                                Collectors.counting()));
                result.put("jobs", Map.of(
                        "total", allJobs.getTotalElements(),
                        "byStatus", jobsByStatus
                ));
            }

            // Critical stock (inventory module)
            if (modules.contains("inventory")) {
                List<CriticalArticleResponse> criticalArticles = stockService.getCriticalArticles();
                result.put("criticalStockCount", criticalArticles.size());
            }

            // Machine status distribution (machines module)
            if (modules.contains("machines")) {
                List<MachineResponse> machines = machineService.getAll();
                Map<String, Long> machinesByStatus = machines.stream()
                        .collect(Collectors.groupingBy(
                                m -> m.status().name(),
                                Collectors.counting()));
                result.put("machines", Map.of(
                        "total", machines.size(),
                        "byStatus", machinesByStatus
                ));
            }

            // Employee count (people module)
            if (modules.contains("people")) {
                Page<EmployeeEntity> employees = employeeService.findAll(Pageable.unpaged());
                result.put("employeeCount", employees.getTotalElements());
            }

            // Open conversations (inbox module)
            if (modules.contains("inbox")) {
                Page<ConversationEntity> openConversations = conversationService.findAll(Pageable.unpaged(), "OPEN");
                result.put("openConversationsCount", openConversations.getTotalElements());
            }

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_kpi_summary: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der KPI-Zusammenfassung: " + e.getMessage());
        }
    }
}
