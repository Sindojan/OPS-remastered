package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.production.JobService;
import com.owlsburg.ops.production.JobStatus;
import com.owlsburg.ops.production.dto.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class UpdateJobStatusTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(UpdateJobStatusTool.class);

    private static final Set<JobStatus> ALLOWED_STATUSES = Set.of(
            JobStatus.IN_PRODUCTION,
            JobStatus.ON_HOLD,
            JobStatus.RELEASED
    );

    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public UpdateJobStatusTool(JobService jobService, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "update_job_status";
    }

    @Override
    public String getDescription() {
        return "Status eines Fertigungsauftrags ändern. Erlaubt: IN_PRODUCTION (In Bearbeitung), ON_HOLD (Pausiert), RELEASED (Freigegeben). COMPLETED und CANCELLED sind nicht erlaubt.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "jobId":{"type":"string","description":"UUID des Auftrags"},
              "newStatus":{"type":"string","enum":["IN_PRODUCTION","ON_HOLD","RELEASED"],"description":"Neuer Status"},
              "reason":{"type":"string","description":"Grund für die Statusänderung"}
            },"required":["jobId","newStatus"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public String getModuleId() {
        return "production";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            UUID jobId = UUID.fromString(inputNode.get("jobId").asText());
            JobStatus newStatus = JobStatus.valueOf(inputNode.get("newStatus").asText());
            String reason = inputNode.has("reason") ? inputNode.get("reason").asText() : null;

            if (!ALLOWED_STATUSES.contains(newStatus)) {
                return ToolResult.error("Status " + newStatus + " ist nicht erlaubt. Nur IN_PRODUCTION, ON_HOLD, RELEASED.");
            }

            JobResponse updated = jobService.changeStatus(jobId, newStatus, null, reason);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "Auftrag " + updated.jobNumber() + " auf " + newStatus.name() + " gesetzt.");
            result.put("auftrag", updated.jobNumber());
            result.put("neuerStatus", updated.status().name());

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (IllegalArgumentException e) {
            return ToolResult.error("Ungültiger Statuswechsel: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error executing update_job_status: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Ändern des Status: " + e.getMessage());
        }
    }
}
