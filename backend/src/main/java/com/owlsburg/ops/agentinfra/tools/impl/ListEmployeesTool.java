package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.people.EmployeeEntity;
import com.owlsburg.ops.people.EmployeeService;
import com.owlsburg.ops.people.EmployeeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ListEmployeesTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ListEmployeesTool.class);

    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;

    public ListEmployeesTool(EmployeeService employeeService, ObjectMapper objectMapper) {
        this.employeeService = employeeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_employee_overview";
    }

    @Override
    public String getDescription() {
        return "Gibt eine Übersicht aller Mitarbeiter zurück: Gesamtanzahl, Verteilung nach Status und Rolle.";
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
            Page<EmployeeEntity> employees = employeeService.findAll(Pageable.unpaged());

            Map<String, Long> byStatus = employees.getContent().stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getStatus() != null ? e.getStatus().name() : "UNKNOWN",
                            Collectors.counting()));

            Map<String, Long> byRole = employees.getContent().stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getRole() != null ? e.getRole() : "UNKNOWN",
                            Collectors.counting()));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalCount", employees.getTotalElements());
            result.put("byStatus", byStatus);
            result.put("byRole", byRole);

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_employee_overview: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Mitarbeiterübersicht: " + e.getMessage());
        }
    }
}
