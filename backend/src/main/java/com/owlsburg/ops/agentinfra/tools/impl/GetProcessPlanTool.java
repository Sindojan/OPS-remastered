package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.bom.ProcessPlanEntity;
import com.owlsburg.ops.bom.ProcessPlanService;
import com.owlsburg.ops.bom.ProcessStepEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetProcessPlanTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetProcessPlanTool.class);

    private final ProcessPlanService processPlanService;
    private final ObjectMapper objectMapper;

    public GetProcessPlanTool(ProcessPlanService processPlanService, ObjectMapper objectMapper) {
        this.processPlanService = processPlanService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_process_plan";
    }

    @Override
    public String getDescription() {
        return "Arbeitsplan eines Teils mit allen Arbeitsschritten, Rüst- und Bearbeitungszeiten abrufen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "planId":{"type":"string","description":"UUID des Arbeitsplans"}
            },"required":["planId"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public String getModuleId() {
        return "bom";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            UUID planId = UUID.fromString(inputNode.get("planId").asText());

            ProcessPlanEntity plan = processPlanService.getPlanById(planId);
            List<ProcessStepEntity> steps = processPlanService.getSteps(planId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", plan.getId().toString());
            result.put("name", plan.getName());
            result.put("version", plan.getVersionNumber());
            result.put("status", plan.getStatus().name());

            int totalSetup = 0;
            int totalProcessing = 0;
            List<Map<String, Object>> stepList = new java.util.ArrayList<>();
            for (ProcessStepEntity step : steps) {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("schritt", step.getStepNumber());
                sm.put("name", step.getName());
                sm.put("beschreibung", step.getDescription());
                sm.put("rüstzeitMin", step.getSetupTimeMinutes());
                sm.put("bearbeitungszeitMin", step.getProcessingTimeMinutes());
                sm.put("notizen", step.getNotes());
                stepList.add(sm);
                totalSetup += step.getSetupTimeMinutes();
                totalProcessing += step.getProcessingTimeMinutes();
            }

            result.put("schritte", stepList);
            result.put("anzahlSchritte", steps.size());
            result.put("gesamtRüstzeit", totalSetup);
            result.put("gesamtBearbeitungszeit", totalProcessing);
            result.put("gesamtzeitMin", totalSetup + totalProcessing);

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_process_plan: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden des Arbeitsplans: " + e.getMessage());
        }
    }
}
