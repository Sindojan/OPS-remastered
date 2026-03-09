package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.machines.MachineService;
import com.owlsburg.ops.machines.MachineStatus;
import com.owlsburg.ops.machines.MaintenanceService;
import com.owlsburg.ops.machines.dto.MachineResponse;
import com.owlsburg.ops.machines.dto.MaintenanceIntervalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GetMachinesTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetMachinesTool.class);

    private final MachineService machineService;
    private final MaintenanceService maintenanceService;
    private final ObjectMapper objectMapper;

    public GetMachinesTool(MachineService machineService,
                           MaintenanceService maintenanceService,
                           ObjectMapper objectMapper) {
        this.machineService = machineService;
        this.maintenanceService = maintenanceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_machines";
    }

    @Override
    public String getDescription() {
        return "Maschinenstatus abrufen, optional gefiltert nach Status oder überfälligen Wartungen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "status":{"type":"string","enum":["AVAILABLE","IN_USE","MAINTENANCE","BLOCKED","DECOMMISSIONED"],"description":"Filter nach Maschinenstatus"},
              "maintenanceOverdue":{"type":"boolean","description":"Nur Maschinen mit überfälliger Wartung"}
            }}""";
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
            JsonNode inputNode = objectMapper.readTree(input);
            boolean maintenanceOverdue = inputNode.has("maintenanceOverdue")
                    && inputNode.get("maintenanceOverdue").asBoolean();

            List<MachineResponse> machines = machineService.getAll();

            // Filter by status if provided
            if (inputNode.has("status") && !inputNode.get("status").isNull()) {
                MachineStatus status = MachineStatus.valueOf(inputNode.get("status").asText());
                machines = machines.stream()
                        .filter(m -> m.status() == status)
                        .toList();
            }

            // Get overdue maintenance machine IDs if needed
            Set<UUID> overdueIds = Set.of();
            List<MaintenanceIntervalResponse> overdueIntervals = maintenanceService.getOverdue();
            if (maintenanceOverdue || !overdueIntervals.isEmpty()) {
                overdueIds = overdueIntervals.stream()
                        .map(MaintenanceIntervalResponse::machineId)
                        .collect(Collectors.toSet());
            }

            if (maintenanceOverdue) {
                Set<UUID> finalOverdueIds = overdueIds;
                machines = machines.stream()
                        .filter(m -> finalOverdueIds.contains(m.id()))
                        .toList();
            }

            Set<UUID> finalOverdueIds2 = overdueIds;
            List<Map<String, Object>> result = machines.stream()
                    .map(m -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", m.id().toString());
                        map.put("name", m.name());
                        map.put("typ", m.type());
                        map.put("status", m.status().name());
                        map.put("wartungÜberfällig", finalOverdueIds2.contains(m.id()));
                        return map;
                    })
                    .toList();

            String json = objectMapper.writeValueAsString(Map.of(
                    "anzahl", result.size(),
                    "maschinen", result
            ));
            if (json.length() > 2000) {
                json = json.substring(0, 1950) + "...\n[Ergebnis gekürzt]";
            }
            return ToolResult.success(json);
        } catch (Exception e) {
            log.error("Error executing get_machines: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Maschinen: " + e.getMessage());
        }
    }
}
