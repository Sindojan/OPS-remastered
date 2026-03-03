package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.messaging.AgentMessageBus;
import com.owlsburg.ops.agentinfra.messaging.AgentMessageEntity;
import com.owlsburg.ops.agentinfra.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckMessagesTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(CheckMessagesTool.class);

    private final AgentMessageBus messageBus;
    private final ObjectMapper objectMapper;

    public CheckMessagesTool(AgentMessageBus messageBus, ObjectMapper objectMapper) {
        this.messageBus = messageBus;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "check_messages";
    }

    @Override
    public String getDescription() {
        return "Prüft ungelesene Nachrichten von anderen Agents. Zeigt eingehende Berichte, Anfragen und Informationen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{}}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            List<AgentMessageEntity> messages = messageBus.getUnreadMessages(context.instanceId());

            if (messages.isEmpty()) {
                return ToolResult.success("{\"messages\":[],\"count\":0}");
            }

            StringBuilder sb = new StringBuilder("{\"messages\":[");
            for (int i = 0; i < messages.size(); i++) {
                AgentMessageEntity msg = messages.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"type\":\"").append(msg.getMessageType())
                  .append("\",\"priority\":\"").append(msg.getPriority())
                  .append("\",\"subject\":").append(objectMapper.writeValueAsString(msg.getSubject()))
                  .append(",\"body\":").append(objectMapper.writeValueAsString(msg.getBody()))
                  .append(",\"from\":\"").append(msg.getSenderInstanceId())
                  .append("\",\"at\":\"").append(msg.getCreatedAt())
                  .append("\"}");
            }
            sb.append("],\"count\":").append(messages.size()).append("}");

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error checking messages: {}", e.getMessage());
            return ToolResult.error("Fehler beim Abrufen: " + e.getMessage());
        }
    }
}
