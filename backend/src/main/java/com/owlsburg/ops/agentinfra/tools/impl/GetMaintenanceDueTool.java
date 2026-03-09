package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.machines.MaintenanceService;
import com.owlsburg.ops.machines.dto.MaintenanceIntervalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetMaintenanceDueTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetMaintenanceDueTool.class);

    private final MaintenanceService maintenanceService;
    private final ObjectMapper objectMapper;

    public GetMaintenanceDueTool(MaintenanceService maintenanceService, ObjectMapper objectMapper) {
        this.maintenanceService = maintenanceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_maintenance_due";
    }

    @Override
    public String getDescription() {
        return "Zeigt überfällige und in den nächsten 7 Tagen anstehende Wartungen an.";
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
    public String getModuleId() {
        return "machines";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            List<MaintenanceIntervalResponse> overdue = maintenanceService.getOverdue();
            List<MaintenanceIntervalResponse> upcoming = maintenanceService.getUpcoming(7);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("overdue", overdue.stream().map(this::toMap).toList());
            result.put("upcomingWeek", upcoming.stream().map(this::toMap).toList());

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_maintenance_due: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Wartungsübersicht: " + e.getMessage());
        }
    }

    private Map<String, Object> toMap(MaintenanceIntervalResponse interval) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", interval.id().toString());
        m.put("machineId", interval.machineId().toString());
        m.put("type", interval.type().name());
        m.put("intervalDays", interval.intervalDays());
        m.put("intervalHours", interval.intervalHours());
        m.put("lastPerformedAt", interval.lastPerformedAt() != null ? interval.lastPerformedAt().toString() : null);
        m.put("nextDueAt", interval.nextDueAt() != null ? interval.nextDueAt().toString() : null);
        m.put("description", interval.description());
        return m;
    }
}
