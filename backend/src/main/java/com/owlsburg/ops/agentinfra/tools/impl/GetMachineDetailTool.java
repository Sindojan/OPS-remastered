package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.machines.MachineIncidentService;
import com.owlsburg.ops.machines.MachineService;
import com.owlsburg.ops.machines.MaintenanceService;
import com.owlsburg.ops.machines.dto.MachineIncidentResponse;
import com.owlsburg.ops.machines.dto.MachineResponse;
import com.owlsburg.ops.machines.dto.MaintenanceIntervalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetMachineDetailTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetMachineDetailTool.class);

    private final MachineService machineService;
    private final MachineIncidentService incidentService;
    private final MaintenanceService maintenanceService;
    private final ObjectMapper objectMapper;

    public GetMachineDetailTool(MachineService machineService,
                                MachineIncidentService incidentService,
                                MaintenanceService maintenanceService,
                                ObjectMapper objectMapper) {
        this.machineService = machineService;
        this.incidentService = incidentService;
        this.maintenanceService = maintenanceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_machine_detail";
    }

    @Override
    public String getDescription() {
        return "Detail einer Maschine inklusive letzter Störungen und geplanter Wartungen abrufen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "machineId":{"type":"string","description":"UUID der Maschine"}
            },"required":["machineId"]}""";
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
            UUID machineId = UUID.fromString(inputNode.get("machineId").asText());

            MachineResponse machine = machineService.getById(machineId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", machine.id().toString());
            result.put("name", machine.name());
            result.put("nummer", machine.machineNumber());
            result.put("typ", machine.type());
            result.put("status", machine.status().name());
            result.put("hersteller", machine.manufacturer());
            result.put("modell", machine.model());
            result.put("seriennummer", machine.serialNumber());
            result.put("kaufdatum", machine.purchaseDate() != null ? machine.purchaseDate().toString() : null);

            // Letzte 3 Störungen
            List<MachineIncidentResponse> incidents = incidentService.findByMachine(machineId);
            List<Map<String, Object>> incidentList = incidents.stream()
                    .sorted(Comparator.comparing(MachineIncidentResponse::reportedAt).reversed())
                    .limit(3)
                    .map(inc -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("typ", inc.type());
                        m.put("beschreibung", inc.description());
                        m.put("schweregrad", inc.severity().name());
                        m.put("gemeldetAm", inc.reportedAt().toString());
                        m.put("gelöst", inc.resolvedAt() != null);
                        if (inc.resolvedAt() != null) {
                            m.put("gelöstAm", inc.resolvedAt().toString());
                        }
                        return m;
                    })
                    .toList();
            result.put("letzteStörungen", incidentList);

            // Nächste geplante Wartung
            List<MaintenanceIntervalResponse> intervals = maintenanceService.getIntervalsByMachine(machineId);
            intervals.stream()
                    .filter(i -> i.nextDueAt() != null)
                    .min(Comparator.comparing(MaintenanceIntervalResponse::nextDueAt))
                    .ifPresent(next -> {
                        Map<String, Object> maint = new LinkedHashMap<>();
                        maint.put("typ", next.type().name());
                        maint.put("beschreibung", next.description());
                        maint.put("fälligAm", next.nextDueAt().toString());
                        maint.put("letzteWartung", next.lastPerformedAt() != null ? next.lastPerformedAt().toString() : null);
                        result.put("nächsteWartung", maint);
                    });

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_machine_detail: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Maschinendetails: " + e.getMessage());
        }
    }
}
