package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.common.SeverityLevel;
import com.owlsburg.ops.machines.MachineIncidentService;
import com.owlsburg.ops.machines.dto.MachineIncidentResponse;
import com.owlsburg.ops.machines.dto.ReportIncidentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class ReportIncidentTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ReportIncidentTool.class);

    private final MachineIncidentService incidentService;
    private final ObjectMapper objectMapper;

    public ReportIncidentTool(MachineIncidentService incidentService, ObjectMapper objectMapper) {
        this.incidentService = incidentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "report_incident";
    }

    @Override
    public String getDescription() {
        return "Meldet eine Maschinenstörung. Benötigt Maschinen-ID, Typ, Beschreibung und Schweregrad.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "machineId":{"type":"string","description":"UUID der Maschine"},
              "type":{"type":"string","description":"Art der Störung (z.B. MECHANICAL, ELECTRICAL, SOFTWARE)"},
              "description":{"type":"string","description":"Beschreibung der Störung"},
              "severity":{"type":"string","enum":["LOW","MEDIUM","HIGH","CRITICAL"],"description":"Schweregrad"}
            },"required":["machineId","type","severity"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public String getModuleId() {
        return "machines";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            UUID machineId = UUID.fromString(node.get("machineId").asText());
            String type = node.get("type").asText();
            String description = node.has("description") ? node.get("description").asText() : "";
            SeverityLevel severity = SeverityLevel.valueOf(node.get("severity").asText());

            ReportIncidentRequest request = new ReportIncidentRequest(null, type, description, severity);
            MachineIncidentResponse response = incidentService.report(machineId, request);

            return ToolResult.success(objectMapper.writeValueAsString(Map.of(
                    "status", "gemeldet",
                    "incidentId", response.id().toString(),
                    "maschine", machineId.toString(),
                    "schweregrad", severity.name()
            )));
        } catch (Exception e) {
            log.error("Error executing report_incident: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Melden der Störung: " + e.getMessage());
        }
    }
}
