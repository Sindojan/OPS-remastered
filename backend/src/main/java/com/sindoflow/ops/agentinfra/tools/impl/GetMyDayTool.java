package com.sindoflow.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sindoflow.ops.agentinfra.tools.*;
import com.sindoflow.ops.people.TimeTrackingService;
import com.sindoflow.ops.people.dto.MyDayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GetMyDayTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetMyDayTool.class);

    private final TimeTrackingService timeTrackingService;
    private final ObjectMapper objectMapper;

    public GetMyDayTool(TimeTrackingService timeTrackingService, ObjectMapper objectMapper) {
        this.timeTrackingService = timeTrackingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_my_day";
    }

    @Override
    public String getDescription() {
        return "Gibt den Tagesüberblick eines Mitarbeiters zurück: Stempelstatus (ein-/ausgestempelt) und alle Zeiteinträge des Tages.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{\"employeeId\":{\"type\":\"string\",\"description\":\"UUID des Mitarbeiters\"}},\"required\":[\"employeeId\"]}";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            String employeeIdStr = inputNode.get("employeeId").asText();
            UUID employeeId = UUID.fromString(employeeIdStr);

            MyDayResponse myDay = timeTrackingService.getMyDay(employeeId);

            return ToolResult.success(objectMapper.writeValueAsString(myDay));
        } catch (Exception e) {
            log.error("Error executing get_my_day: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden des Tagesüberblicks: " + e.getMessage());
        }
    }
}
