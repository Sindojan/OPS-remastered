package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.machines.MaintenanceService;
import com.owlsburg.ops.machines.MaintenanceType;
import com.owlsburg.ops.machines.dto.CreateMaintenanceIntervalRequest;
import com.owlsburg.ops.machines.dto.MaintenanceIntervalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Component
public class ScheduleMaintenanceTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ScheduleMaintenanceTool.class);

    private final MaintenanceService maintenanceService;
    private final ObjectMapper objectMapper;

    public ScheduleMaintenanceTool(MaintenanceService maintenanceService, ObjectMapper objectMapper) {
        this.maintenanceService = maintenanceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "schedule_maintenance";
    }

    @Override
    public String getDescription() {
        return "Plant eine Maschinenwartung. Erstellt ein zeitbasiertes Wartungsintervall.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "machineId":{"type":"string","description":"UUID der Maschine"},
              "intervalDays":{"type":"integer","description":"Wartungsintervall in Tagen"},
              "description":{"type":"string","description":"Beschreibung der Wartungsmaßnahme"}
            },"required":["machineId","intervalDays"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            UUID machineId = UUID.fromString(node.get("machineId").asText());
            int intervalDays = node.get("intervalDays").asInt();
            String description = node.has("description") ? node.get("description").asText() : "Geplante Wartung";

            Instant nextDue = Instant.now().plus(intervalDays, ChronoUnit.DAYS);

            CreateMaintenanceIntervalRequest request = new CreateMaintenanceIntervalRequest(
                    machineId, MaintenanceType.TIME_BASED, intervalDays, null, nextDue, description);
            MaintenanceIntervalResponse response = maintenanceService.createInterval(request);

            return ToolResult.success(objectMapper.writeValueAsString(Map.of(
                    "status", "geplant",
                    "intervalId", response.id().toString(),
                    "maschine", machineId.toString(),
                    "intervallTage", intervalDays,
                    "nächsteFälligkeit", nextDue.toString()
            )));
        } catch (Exception e) {
            log.error("Error executing schedule_maintenance: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Planen der Wartung: " + e.getMessage());
        }
    }
}
