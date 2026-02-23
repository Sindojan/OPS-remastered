package com.sindoflow.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sindoflow.ops.agentinfra.tools.*;
import com.sindoflow.ops.machines.MachineService;
import com.sindoflow.ops.machines.dto.MachineResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ListMachinesTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ListMachinesTool.class);

    private final MachineService machineService;
    private final ObjectMapper objectMapper;

    public ListMachinesTool(MachineService machineService, ObjectMapper objectMapper) {
        this.machineService = machineService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_machine_overview";
    }

    @Override
    public String getDescription() {
        return "Listet alle Maschinen mit Status, Nummer und zugeordneter Station auf.";
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
            List<MachineResponse> machines = machineService.getAll();

            List<Map<String, Object>> result = machines.stream()
                    .map(machine -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", machine.id().toString());
                        m.put("name", machine.name());
                        m.put("machineNumber", machine.machineNumber());
                        m.put("status", machine.status().name());
                        m.put("stationId", machine.stationId() != null ? machine.stationId().toString() : null);
                        return m;
                    })
                    .toList();

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_machine_overview: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Maschinenübersicht: " + e.getMessage());
        }
    }
}
