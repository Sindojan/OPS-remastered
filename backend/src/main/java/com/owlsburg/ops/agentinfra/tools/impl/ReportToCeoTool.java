package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentInstanceRepository;
import com.owlsburg.ops.agentinfra.messaging.AgentMessageBus;
import com.owlsburg.ops.agentinfra.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ReportToCeoTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ReportToCeoTool.class);

    private final AgentMessageBus messageBus;
    private final AgentInstanceRepository instanceRepository;
    private final ObjectMapper objectMapper;

    public ReportToCeoTool(AgentMessageBus messageBus,
                           AgentInstanceRepository instanceRepository,
                           ObjectMapper objectMapper) {
        this.messageBus = messageBus;
        this.instanceRepository = instanceRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "report_to_ceo";
    }

    @Override
    public String getDescription() {
        return "Sendet einen Bericht an den CEO-Agent. Nutze dies für wichtige Statusmeldungen oder Eskalationen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "subject":{"type":"string","description":"Betreff des Berichts"},
              "body":{"type":"string","description":"Berichtsinhalt"}
            },"required":["subject","body"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String subject = node.get("subject").asText();
            String body = node.get("body").asText();

            // Find CEO instance (agent with no parent)
            UUID tenantId = UUID.fromString(context.tenantId());
            List<AgentInstanceEntity> instances = instanceRepository.findByTenantId(tenantId);
            AgentInstanceEntity ceo = instances.stream()
                    .filter(i -> i.getParentInstanceId() == null)
                    .findFirst()
                    .orElse(null);

            if (ceo == null) {
                return ToolResult.error("CEO-Agent nicht gefunden");
            }

            messageBus.send(context.instanceId(), ceo.getId(), "REPORT", subject, body, "NORMAL");

            return ToolResult.success("{\"sent\":true,\"target\":\"CEO Agent\"}");
        } catch (Exception e) {
            log.error("Error reporting to CEO: {}", e.getMessage());
            return ToolResult.error("Fehler beim Senden: " + e.getMessage());
        }
    }
}
