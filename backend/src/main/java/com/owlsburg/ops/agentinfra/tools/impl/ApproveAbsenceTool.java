package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.people.AbsenceEntity;
import com.owlsburg.ops.people.AbsenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class ApproveAbsenceTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ApproveAbsenceTool.class);

    private final AbsenceService absenceService;
    private final ObjectMapper objectMapper;

    public ApproveAbsenceTool(AbsenceService absenceService, ObjectMapper objectMapper) {
        this.absenceService = absenceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "approve_absence";
    }

    @Override
    public String getDescription() {
        return "Genehmigt oder lehnt einen Abwesenheitsantrag ab.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "absenceId":{"type":"string","description":"UUID des Abwesenheitsantrags"},
              "action":{"type":"string","enum":["APPROVE","REJECT"],"description":"Aktion: Genehmigen oder Ablehnen"}
            },"required":["absenceId","action"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            UUID absenceId = UUID.fromString(node.get("absenceId").asText());
            String action = node.get("action").asText();

            AbsenceEntity result;
            if ("APPROVE".equals(action)) {
                result = absenceService.approve(absenceId);
            } else if ("REJECT".equals(action)) {
                result = absenceService.reject(absenceId);
            } else {
                return ToolResult.error("Ungültige Aktion: " + action + ". Erlaubt: APPROVE, REJECT");
            }

            return ToolResult.success(objectMapper.writeValueAsString(Map.of(
                    "status", result.getStatus().name(),
                    "absenceId", absenceId.toString(),
                    "aktion", action
            )));
        } catch (Exception e) {
            log.error("Error executing approve_absence: {}", e.getMessage(), e);
            return ToolResult.error("Fehler bei der Bearbeitung des Abwesenheitsantrags: " + e.getMessage());
        }
    }
}
