package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentInstanceRepository;
import com.owlsburg.ops.agentinfra.AgentTemplateEntity;
import com.owlsburg.ops.agentinfra.AgentTemplateRepository;
import com.owlsburg.ops.agentinfra.execution.LeadAgentRunner;
import com.owlsburg.ops.agentinfra.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DelegateToLeadTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(DelegateToLeadTool.class);

    private static final Map<String, String> LEAD_ROLE_MAPPING = Map.of(
            "produktions_lead", "production_lead",
            "maschinen_lead", "machine_lead",
            "lager_lead", "supply_lead",
            "personal_lead", "people_lead",
            "support_lead", "support_lead"
    );

    private final AgentTemplateRepository templateRepository;
    private final AgentInstanceRepository instanceRepository;
    private final LeadAgentRunner leadAgentRunner;
    private final ObjectMapper objectMapper;

    public DelegateToLeadTool(AgentTemplateRepository templateRepository,
                              AgentInstanceRepository instanceRepository,
                              LeadAgentRunner leadAgentRunner,
                              ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.instanceRepository = instanceRepository;
        this.leadAgentRunner = leadAgentRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "delegate_to_lead";
    }

    @Override
    public String getDescription() {
        return "Delegiert eine Aufgabe an einen spezialisierten Lead-Agent. " +
                "Verfügbare Leads: produktions_lead, maschinen_lead, lager_lead, personal_lead, support_lead.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "lead":{"type":"string","enum":["produktions_lead","maschinen_lead","lager_lead","personal_lead","support_lead"],"description":"Name des Lead-Agents"},
              "task":{"type":"string","description":"Aufgabenbeschreibung für den Lead"},
              "priority":{"type":"string","enum":["low","normal","high"],"description":"Priorität (optional)"}
            },"required":["lead","task"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String leadName = node.get("lead").asText();
            String task = node.get("task").asText();

            // Resolve role from mapping
            String role = LEAD_ROLE_MAPPING.get(leadName);
            if (role == null) {
                return ToolResult.error("Unbekannter Lead: " + leadName
                        + ". Verfügbar: " + LEAD_ROLE_MAPPING.keySet());
            }

            UUID tenantId = UUID.fromString(context.tenantId());

            // Find template by role and tenant
            AgentTemplateEntity template = templateRepository.findByRoleAndTenantId(role, tenantId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Kein Template für Rolle '" + role + "' gefunden"));

            // Find active instance for this template
            List<AgentInstanceEntity> instances = instanceRepository.findByTemplateId(template.getId());
            if (instances.isEmpty()) {
                return ToolResult.error("Keine aktive Instanz für " + leadName + " gefunden");
            }
            AgentInstanceEntity leadInstance = instances.get(0);

            log.info("Delegating to {} (role: {}, instance: {}): {}",
                    leadName, role, leadInstance.getId(), task);

            // Run lead agent synchronously
            long startTime = System.currentTimeMillis();
            String result = leadAgentRunner.runLead(leadInstance, task, context.tenantId());
            long elapsed = System.currentTimeMillis() - startTime;

            log.info("Delegation to {} completed in {}ms (result length: {})",
                    leadName, elapsed, result != null ? result.length() : 0);

            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error executing delegate_to_lead: {}", e.getMessage(), e);
            return ToolResult.error("Fehler bei der Delegation: " + e.getMessage());
        }
    }
}
