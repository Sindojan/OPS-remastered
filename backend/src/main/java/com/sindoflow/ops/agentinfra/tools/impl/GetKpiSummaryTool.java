package com.sindoflow.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sindoflow.ops.agentinfra.tools.*;
import com.sindoflow.ops.inbox.ConversationEntity;
import com.sindoflow.ops.inbox.ConversationService;
import com.sindoflow.ops.inventory.StockService;
import com.sindoflow.ops.inventory.dto.CriticalArticleResponse;
import com.sindoflow.ops.machines.MachineService;
import com.sindoflow.ops.machines.dto.MachineResponse;
import com.sindoflow.ops.people.EmployeeEntity;
import com.sindoflow.ops.people.EmployeeService;
import com.sindoflow.ops.production.JobService;
import com.sindoflow.ops.production.JobStatus;
import com.sindoflow.ops.production.dto.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GetKpiSummaryTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetKpiSummaryTool.class);

    private final JobService jobService;
    private final StockService stockService;
    private final MachineService machineService;
    private final EmployeeService employeeService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public GetKpiSummaryTool(JobService jobService,
                              StockService stockService,
                              MachineService machineService,
                              EmployeeService employeeService,
                              ConversationService conversationService,
                              ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.stockService = stockService;
        this.machineService = machineService;
        this.employeeService = employeeService;
        this.conversationService = conversationService;
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
            // Jobs by status
            Page<JobResponse> allJobs = jobService.getAll(Pageable.unpaged());
            Map<String, Long> jobsByStatus = allJobs.getContent().stream()
                    .collect(Collectors.groupingBy(
                            job -> job.status().name(),
                            Collectors.counting()));

            // Critical stock
            List<CriticalArticleResponse> criticalArticles = stockService.getCriticalArticles();

            // Machine status distribution
            List<MachineResponse> machines = machineService.getAll();
            Map<String, Long> machinesByStatus = machines.stream()
                    .collect(Collectors.groupingBy(
                            m -> m.status().name(),
                            Collectors.counting()));

            // Employee count
            Page<EmployeeEntity> employees = employeeService.findAll(Pageable.unpaged());

            // Open conversations
            Page<ConversationEntity> openConversations = conversationService.findAll(Pageable.unpaged(), "OPEN");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("jobs", Map.of(
                    "total", allJobs.getTotalElements(),
                    "byStatus", jobsByStatus
            ));
            result.put("criticalStockCount", criticalArticles.size());
            result.put("machines", Map.of(
                    "total", machines.size(),
                    "byStatus", machinesByStatus
            ));
            result.put("employeeCount", employees.getTotalElements());
            result.put("openConversationsCount", openConversations.getTotalElements());

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_kpi_summary: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der KPI-Zusammenfassung: " + e.getMessage());
        }
    }
}
