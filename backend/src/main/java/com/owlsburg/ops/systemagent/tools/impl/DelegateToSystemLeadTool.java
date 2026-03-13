package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.runtime.AgentActivityEvent;
import com.owlsburg.ops.agentinfra.runtime.LeadStep;
import com.owlsburg.ops.agentinfra.runtime.RunMemory;
import com.owlsburg.ops.systemagent.*;
import com.owlsburg.ops.systemagent.runtime.*;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DelegateToSystemLeadTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(DelegateToSystemLeadTool.class);

    private static final Map<String, String> LEAD_ROLE_MAPPING = Map.of(
            "marketing_lead", "marketing_lead",
            "marktforschung_lead", "market_research_lead",
            "kunden_verwalter_lead", "customer_admin_lead",
            "developer_lead", "developer_lead",
            "infrastructure_lead", "infrastructure_lead",
            "security_lead", "security_lead",
            "customer_success_lead", "customer_success_lead",
            "knowledge_lead", "knowledge_lead"
    );

    private final SystemAgentTemplateRepository templateRepository;
    private final SystemAgentInstanceRepository instanceRepository;
    private final SystemAgentFactory agentFactory;
    private final ObjectMapper objectMapper;

    public DelegateToSystemLeadTool(SystemAgentTemplateRepository templateRepository,
                                     SystemAgentInstanceRepository instanceRepository,
                                     SystemAgentFactory agentFactory,
                                     ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.instanceRepository = instanceRepository;
        this.agentFactory = agentFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "delegate_to_system_lead";
    }

    @Override
    public String getDescription() {
        return "Delegiert eine Aufgabe an einen spezialisierten System-Lead-Agent. " +
                "Verfügbare Leads: marketing_lead, marktforschung_lead, kunden_verwalter_lead, " +
                "developer_lead, infrastructure_lead, security_lead, customer_success_lead, knowledge_lead.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "lead":{"type":"string","enum":["marketing_lead","marktforschung_lead","kunden_verwalter_lead","developer_lead","infrastructure_lead","security_lead","customer_success_lead","knowledge_lead"],"description":"Name des System-Lead-Agents"},
              "task":{"type":"string","description":"Aufgabenbeschreibung für den Lead"}
            },"required":["lead","task"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String leadName = node.get("lead").asText();
            String task = node.get("task").asText();

            String role = LEAD_ROLE_MAPPING.get(leadName);
            if (role == null) {
                return SystemToolResult.error("Unbekannter System-Lead: " + leadName
                        + ". Verfügbar: " + LEAD_ROLE_MAPPING.keySet());
            }

            SystemAgentTemplateEntity template = templateRepository.findByRole(role)
                    .orElseThrow(() -> new IllegalStateException(
                            "Kein System-Template für Rolle '" + role + "' gefunden"));

            List<SystemAgentInstanceEntity> instances = instanceRepository.findByTemplateId(template.getId());
            if (instances.isEmpty()) {
                return SystemToolResult.error("Keine aktive System-Instanz für " + leadName + " gefunden");
            }
            SystemAgentInstanceEntity leadInstance = instances.get(0);

            log.info("System delegation to {} (role: {}, instance: {}): {}",
                    leadName, role, leadInstance.getId(), task);

            // Publish DELEGATION_START
            if (context.activityBus() != null) {
                try {
                    context.activityBus().publish(new AgentActivityEvent(
                            AgentActivityEvent.Type.DELEGATION_START,
                            context.instanceId(), leadInstance.getId(),
                            "System CEO", leadName, Instant.now()));
                } catch (Exception e) {
                    log.debug("Failed to publish DELEGATION_START: {}", e.getMessage());
                }
            }

            long startTime = System.currentTimeMillis();
            SystemAgent leadAgent = agentFactory.createAgent(leadInstance.getId());
            SystemAgentContext agentContext = new SystemAgentContext(
                    UUID.randomUUID(), null, null, 1, Instant.now(),
                    context.activityBus(), new RunMemory());
            SystemAgentResult result = leadAgent.execute(agentContext, task);
            long elapsed = System.currentTimeMillis() - startTime;

            // Publish DELEGATION_END
            if (context.activityBus() != null) {
                try {
                    context.activityBus().publish(new AgentActivityEvent(
                            AgentActivityEvent.Type.DELEGATION_END,
                            context.instanceId(), leadInstance.getId(),
                            "System CEO", leadName, Instant.now()));
                } catch (Exception e) {
                    log.debug("Failed to publish DELEGATION_END: {}", e.getMessage());
                }
            }

            log.info("System delegation to {} completed in {}ms (status: {}, tokens: {}+{})",
                    leadName, elapsed, result.status(), result.inputTokens(), result.outputTokens());

            // Build structured result with steps
            ObjectNode resultJson = objectMapper.createObjectNode();
            resultJson.put("output", result.output());
            var stepsArray = resultJson.putArray("steps");
            for (LeadStep step : result.steps()) {
                ObjectNode stepNode = stepsArray.addObject();
                stepNode.put("type", step.type());
                if (step.toolName() != null) {
                    stepNode.put("toolName", step.toolName());
                }
                stepNode.put("content", step.content());
                stepNode.put("iteration", step.iteration());
            }

            return SystemToolResult.success(objectMapper.writeValueAsString(resultJson));
        } catch (Exception e) {
            log.error("Error executing delegate_to_system_lead: {}", e.getMessage(), e);
            return SystemToolResult.error("Fehler bei der System-Delegation: " + e.getMessage());
        }
    }
}
