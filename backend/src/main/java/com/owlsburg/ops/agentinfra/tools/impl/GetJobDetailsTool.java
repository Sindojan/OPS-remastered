package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.production.JobService;
import com.owlsburg.ops.production.dto.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class GetJobDetailsTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetJobDetailsTool.class);

    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public GetJobDetailsTool(JobService jobService, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_job_details";
    }

    @Override
    public String getDescription() {
        return "Gibt die vollständigen Details eines Produktionsauftrags zurück, inklusive Statushistorie.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{\"jobId\":{\"type\":\"string\",\"description\":\"UUID des Auftrags\"}},\"required\":[\"jobId\"]}";
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
            JsonNode inputNode = objectMapper.readTree(input);
            String jobIdStr = inputNode.get("jobId").asText();
            UUID jobId = UUID.fromString(jobIdStr);

            JobResponse job = jobService.getById(jobId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", job.id().toString());
            result.put("jobNumber", job.jobNumber());
            result.put("customerId", job.customerId() != null ? job.customerId().toString() : null);
            result.put("title", job.title());
            result.put("status", job.status().name());
            result.put("priority", job.priority());
            result.put("quantity", job.quantity());
            result.put("deadline", job.deadline() != null ? job.deadline().toString() : null);
            result.put("notes", job.notes());
            result.put("createdBy", job.createdBy() != null ? job.createdBy().toString() : null);
            result.put("assignedStationId", job.assignedStationId() != null ? job.assignedStationId().toString() : null);
            result.put("shiftId", job.shiftId() != null ? job.shiftId().toString() : null);
            result.put("startedAt", job.startedAt() != null ? job.startedAt().toString() : null);
            result.put("completedAt", job.completedAt() != null ? job.completedAt().toString() : null);
            result.put("statusHistory", job.statusHistory());
            result.put("createdAt", job.createdAt() != null ? job.createdAt().toString() : null);
            result.put("updatedAt", job.updatedAt() != null ? job.updatedAt().toString() : null);

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_job_details: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Auftragsdetails: " + e.getMessage());
        }
    }
}
