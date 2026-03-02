package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.production.JobService;
import com.owlsburg.ops.production.dto.JobResponse;
import com.owlsburg.ops.production.dto.JobStatusHistoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetJobDetailTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetJobDetailTool.class);

    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public GetJobDetailTool(JobService jobService, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_job_detail";
    }

    @Override
    public String getDescription() {
        return "Detail eines Fertigungsauftrags inklusive Statushistorie abrufen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "jobId":{"type":"string","description":"UUID des Auftrags"}
            },"required":["jobId"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            UUID jobId = UUID.fromString(inputNode.get("jobId").asText());
            JobResponse job = jobService.getById(jobId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", job.id().toString());
            result.put("nummer", job.jobNumber());
            result.put("titel", job.title());
            result.put("status", job.status().name());
            result.put("priorität", job.priority());
            result.put("menge", job.quantity());
            result.put("deadline", job.deadline() != null ? job.deadline().toString() : null);
            result.put("überfällig", job.overdue());
            result.put("notizen", job.notes());
            result.put("station", job.assignedStationId() != null ? job.assignedStationId().toString() : null);
            result.put("gestartet", job.startedAt() != null ? job.startedAt().toString() : null);
            result.put("abgeschlossen", job.completedAt() != null ? job.completedAt().toString() : null);
            result.put("kundeId", job.customerId() != null ? job.customerId().toString() : null);

            // Statushistorie (letzte 5)
            List<JobStatusHistoryResponse> history = job.statusHistory();
            if (history != null && !history.isEmpty()) {
                List<Map<String, String>> historyList = history.stream()
                        .limit(5)
                        .map(h -> {
                            Map<String, String> hm = new LinkedHashMap<>();
                            hm.put("von", h.fromStatus() != null ? h.fromStatus().name() : "-");
                            hm.put("nach", h.toStatus().name());
                            hm.put("grund", h.reason());
                            hm.put("datum", h.changedAt() != null ? h.changedAt().toString() : null);
                            return hm;
                        })
                        .toList();
                result.put("statusHistorie", historyList);
            }

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_job_detail: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Auftragsdetails: " + e.getMessage());
        }
    }
}
