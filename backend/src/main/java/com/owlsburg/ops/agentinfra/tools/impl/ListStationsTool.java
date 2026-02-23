package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.production.StationService;
import com.owlsburg.ops.production.dto.StationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ListStationsTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ListStationsTool.class);

    private final StationService stationService;
    private final ObjectMapper objectMapper;

    public ListStationsTool(StationService stationService, ObjectMapper objectMapper) {
        this.stationService = stationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_stations";
    }

    @Override
    public String getDescription() {
        return "Listet alle Produktionsstationen mit Kapazitätsinformationen auf.";
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
            List<StationResponse> stations = stationService.getAll();

            List<Map<String, Object>> result = stations.stream()
                    .map(station -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", station.id().toString());
                        m.put("name", station.name());
                        m.put("status", station.status());
                        m.put("capacityPerShift", station.capacityPerShift());
                        m.put("totalCapacity", station.totalCapacity());
                        m.put("shiftCount", station.shiftIds().size());
                        return m;
                    })
                    .toList();

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_stations: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Stationen: " + e.getMessage());
        }
    }
}
