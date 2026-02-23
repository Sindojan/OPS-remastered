package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.production.JobService;
import com.owlsburg.ops.production.JobStatus;
import com.owlsburg.ops.production.StationService;
import com.owlsburg.ops.production.dto.JobResponse;
import com.owlsburg.ops.production.dto.StationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetCapacityTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetCapacityTool.class);

    private final JobService jobService;
    private final StationService stationService;
    private final ObjectMapper objectMapper;

    public GetCapacityTool(JobService jobService, StationService stationService, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.stationService = stationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_capacity_overview";
    }

    @Override
    public String getDescription() {
        return "Gibt eine Kapazitätsübersicht aller Stationen zurück, inklusive aktiver Aufträge und Auslastung in Prozent.";
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
            Page<JobResponse> activeJobs = jobService.findByStatus(JobStatus.IN_PRODUCTION, Pageable.unpaged());

            List<Map<String, Object>> result = stations.stream()
                    .map(station -> {
                        long activeJobCount = activeJobs.getContent().stream()
                                .filter(job -> station.id().equals(job.assignedStationId()))
                                .count();

                        int capacity = station.totalCapacity() > 0 ? station.totalCapacity() :
                                (station.capacityPerShift() != null ? station.capacityPerShift() : 0);

                        double utilization = capacity > 0 ? (activeJobCount * 100.0 / capacity) : 0.0;

                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("stationName", station.name());
                        m.put("capacity", capacity);
                        m.put("activeJobs", activeJobCount);
                        m.put("utilizationPercent", Math.round(utilization * 10.0) / 10.0);
                        return m;
                    })
                    .toList();

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_capacity_overview: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Kapazitätsübersicht: " + e.getMessage());
        }
    }
}
