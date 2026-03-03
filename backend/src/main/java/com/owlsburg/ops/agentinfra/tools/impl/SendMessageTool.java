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
public class SendMessageTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(SendMessageTool.class);

    private final AgentMessageBus messageBus;
    private final AgentInstanceRepository instanceRepository;
    private final ObjectMapper objectMapper;

    public SendMessageTool(AgentMessageBus messageBus,
                           AgentInstanceRepository instanceRepository,
                           ObjectMapper objectMapper) {
        this.messageBus = messageBus;
        this.instanceRepository = instanceRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "send_message";
    }

    @Override
    public String getDescription() {
        return "Sendet eine Nachricht an einen anderen Agent (z.B. an einen anderen Lead für abteilungsübergreifende Koordination).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "target_agent":{"type":"string","description":"Name des Ziel-Agents (z.B. Production Lead, Supply Lead)"},
              "subject":{"type":"string","description":"Betreff der Nachricht"},
              "body":{"type":"string","description":"Nachrichteninhalt"},
              "priority":{"type":"string","enum":["LOW","NORMAL","HIGH","CRITICAL"],"description":"Priorität (optional, Standard: NORMAL)"}
            },"required":["target_agent","subject","body"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String targetAgentName = node.get("target_agent").asText();
            String subject = node.get("subject").asText();
            String body = node.get("body").asText();
            String priority = node.has("priority") ? node.get("priority").asText() : "NORMAL";

            // Find target instance by name
            UUID tenantId = UUID.fromString(context.tenantId());
            List<AgentInstanceEntity> instances = instanceRepository.findByTenantId(tenantId);
            AgentInstanceEntity target = instances.stream()
                    .filter(i -> i.getName().equalsIgnoreCase(targetAgentName))
                    .findFirst()
                    .orElse(null);

            if (target == null) {
                return ToolResult.error("Agent nicht gefunden: " + targetAgentName);
            }

            messageBus.send(context.instanceId(), target.getId(), "INFO", subject, body, priority);

            return ToolResult.success("{\"sent\":true,\"target\":\"" + targetAgentName + "\"}");
        } catch (IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            return ToolResult.error("Fehler beim Senden: " + e.getMessage());
        }
    }
}
