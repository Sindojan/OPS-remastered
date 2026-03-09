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
import java.util.stream.Collectors;

@Component
public class GetProductionKpisTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetProductionKpisTool.class);

    private final JobService jobService;
    private final StationService stationService;
    private final ObjectMapper objectMapper;

    public GetProductionKpisTool(JobService jobService, StationService stationService,
                                  ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.stationService = stationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_production_kpis";
    }

    @Override
    public String getDescription() {
        return "Aggregierte Produktions-KPIs: Aufträge nach Status, überfällige Aufträge, Stations-Auslastung.";
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
        return "production";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            Page<JobResponse> allJobs = jobService.getAll(Pageable.unpaged());

            Map<String, Long> jobsByStatus = allJobs.getContent().stream()
                    .collect(Collectors.groupingBy(j -> j.status().name(), Collectors.counting()));

            long overdueCount = allJobs.getContent().stream()
                    .filter(JobResponse::overdue)
                    .count();

            List<StationResponse> stations = stationService.getAll();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("aufträge_gesamt", allJobs.getTotalElements());
            result.put("aufträge_nach_status", jobsByStatus);
            result.put("überfällige_aufträge", overdueCount);
            result.put("stationen_gesamt", stations.size());
            result.put("stationen", stations.stream().map(s -> Map.of(
                    "name", s.name(),
                    "kapazität", s.totalCapacity()
            )).toList());

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_production_kpis: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Produktions-KPIs: " + e.getMessage());
        }
    }
}
