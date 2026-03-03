package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.inbox.ConversationMessageEntity;
import com.owlsburg.ops.inbox.MessageService;
import com.owlsburg.ops.inbox.SenderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class CreateInboxReplyTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(CreateInboxReplyTool.class);

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public CreateInboxReplyTool(MessageService messageService, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "create_inbox_reply";
    }

    @Override
    public String getDescription() {
        return "Erstellt eine Agent-Antwort in einer Konversation.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "conversationId":{"type":"string","description":"UUID der Konversation"},
              "content":{"type":"string","description":"Inhalt der Antwort"}
            },"required":["conversationId","content"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            UUID conversationId = UUID.fromString(node.get("conversationId").asText());
            String content = node.get("content").asText();

            ConversationMessageEntity message = messageService.addMessage(
                    conversationId, content, SenderType.AGENT, null);

            return ToolResult.success(objectMapper.writeValueAsString(Map.of(
                    "status", "gesendet",
                    "messageId", message.getId().toString(),
                    "konversationId", conversationId.toString()
            )));
        } catch (Exception e) {
            log.error("Error executing create_inbox_reply: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Erstellen der Antwort: " + e.getMessage());
        }
    }
}
