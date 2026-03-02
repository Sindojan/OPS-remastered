package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.production.JobService;
import com.owlsburg.ops.production.JobStatus;
import com.owlsburg.ops.production.dto.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetJobsTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetJobsTool.class);

    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public GetJobsTool(JobService jobService, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_jobs";
    }

    @Override
    public String getDescription() {
        return "Fertigungsaufträge abrufen, optional gefiltert nach Status oder überfälligen Aufträgen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "status":{"type":"string","enum":["DRAFT","RELEASED","IN_PRODUCTION","ON_HOLD","COMPLETED","CANCELLED"],"description":"Filter nach Auftragsstatus"},
              "overdue":{"type":"boolean","description":"Nur überfällige Aufträge anzeigen"},
              "limit":{"type":"integer","description":"Maximale Anzahl (Standard: 20)"}
            }}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            int limit = inputNode.has("limit") ? inputNode.get("limit").asInt(20) : 20;
            boolean overdueOnly = inputNode.has("overdue") && inputNode.get("overdue").asBoolean();
            Pageable pageable = PageRequest.of(0, Math.min(limit, 50));

            Page<JobResponse> jobs;
            if (inputNode.has("status") && !inputNode.get("status").isNull()) {
                JobStatus status = JobStatus.valueOf(inputNode.get("status").asText());
                jobs = jobService.findByStatus(status, pageable);
            } else {
                jobs = jobService.getAll(pageable);
            }

            List<Map<String, Object>> result = jobs.getContent().stream()
                    .filter(job -> !overdueOnly || job.overdue())
                    .map(job -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", job.id().toString());
                        m.put("nummer", job.jobNumber());
                        m.put("titel", job.title());
                        m.put("status", job.status().name());
                        m.put("priorität", job.priority());
                        m.put("deadline", job.deadline() != null ? job.deadline().toString() : null);
                        m.put("überfällig", job.overdue());
                        m.put("station", job.assignedStationId() != null ? job.assignedStationId().toString() : null);
                        return m;
                    })
                    .toList();

            String json = objectMapper.writeValueAsString(Map.of(
                    "anzahl", result.size(),
                    "gesamt", jobs.getTotalElements(),
                    "aufträge", result
            ));
            return truncateResult(json);
        } catch (Exception e) {
            log.error("Error executing get_jobs: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Aufträge: " + e.getMessage());
        }
    }

    private ToolResult truncateResult(String json) {
        if (json.length() <= 2000) return ToolResult.success(json);
        return ToolResult.success(json.substring(0, 1950) + "...\n[Ergebnis gekürzt, nutze Filter für spezifischere Abfragen]");
    }
}
