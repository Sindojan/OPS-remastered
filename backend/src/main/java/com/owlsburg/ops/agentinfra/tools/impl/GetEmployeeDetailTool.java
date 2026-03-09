package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.people.EmployeeEntity;
import com.owlsburg.ops.people.EmployeeQualificationEntity;
import com.owlsburg.ops.people.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetEmployeeDetailTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetEmployeeDetailTool.class);

    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;

    public GetEmployeeDetailTool(EmployeeService employeeService, ObjectMapper objectMapper) {
        this.employeeService = employeeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_employee_detail";
    }

    @Override
    public String getDescription() {
        return "Mitarbeiterdetails mit Qualifikationen abrufen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "employeeId":{"type":"string","description":"UUID des Mitarbeiters"}
            },"required":["employeeId"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public String getModuleId() {
        return "people";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            UUID employeeId = UUID.fromString(node.get("employeeId").asText());

            EmployeeEntity employee = employeeService.getById(employeeId);
            List<EmployeeQualificationEntity> qualifications = employeeService.getQualifications(employeeId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", employee.getId().toString());
            result.put("personalnummer", employee.getEmployeeNumber());
            result.put("vorname", employee.getFirstName());
            result.put("nachname", employee.getLastName());
            result.put("email", employee.getEmail());
            result.put("rolle", employee.getRole() != null ? employee.getRole() : "");
            result.put("status", employee.getStatus().name());
            result.put("eintrittsdatum", employee.getHireDate() != null ? employee.getHireDate().toString() : null);
            result.put("qualifikationen", qualifications.stream()
                    .map(q -> {
                        Map<String, Object> qMap = new LinkedHashMap<>();
                        qMap.put("qualifikation", q.getQualification());
                        qMap.put("zertifiziert", q.getCertifiedAt() != null ? q.getCertifiedAt().toString() : null);
                        qMap.put("ablaufdatum", q.getExpiresAt() != null ? q.getExpiresAt().toString() : null);
                        return qMap;
                    })
                    .toList());

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_employee_detail: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Mitarbeiterdetails: " + e.getMessage());
        }
    }
}
