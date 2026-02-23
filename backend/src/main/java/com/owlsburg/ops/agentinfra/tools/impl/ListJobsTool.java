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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ListJobsTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ListJobsTool.class);

    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public ListJobsTool(JobService jobService, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "list_jobs";
    }

    @Override
    public String getDescription() {
        return "Listet alle Produktionsaufträge auf, optional gefiltert nach Status (DRAFT, RELEASED, IN_PRODUCTION, ON_HOLD, COMPLETED, CANCELLED).";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{\"status\":{\"type\":\"string\",\"description\":\"Filter nach Status (DRAFT, RELEASED, IN_PRODUCTION, ON_HOLD, COMPLETED, CANCELLED)\"}}}";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            Page<JobResponse> jobs;

            if (inputNode.has("status") && !inputNode.get("status").isNull()) {
                String statusStr = inputNode.get("status").asText();
                JobStatus status = JobStatus.valueOf(statusStr);
                jobs = jobService.findByStatus(status, Pageable.unpaged());
            } else {
                jobs = jobService.getAll(Pageable.unpaged());
            }

            List<Map<String, Object>> result = jobs.getContent().stream()
                    .map(job -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", job.id().toString());
                        m.put("jobNumber", job.jobNumber());
                        m.put("title", job.title());
                        m.put("status", job.status().name());
                        m.put("priority", job.priority());
                        m.put("deadline", job.deadline() != null ? job.deadline().toString() : null);
                        return m;
                    })
                    .toList();

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing list_jobs: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Aufträge: " + e.getMessage());
        }
    }
}
