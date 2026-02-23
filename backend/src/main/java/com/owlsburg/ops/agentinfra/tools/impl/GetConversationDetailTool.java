package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.inbox.ConversationEntity;
import com.owlsburg.ops.inbox.ConversationMessageEntity;
import com.owlsburg.ops.inbox.ConversationService;
import com.owlsburg.ops.inbox.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetConversationDetailTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetConversationDetailTool.class);

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public GetConversationDetailTool(ConversationService conversationService,
                                      MessageService messageService,
                                      ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_conversation_detail";
    }

    @Override
    public String getDescription() {
        return "Gibt die Details einer Konversation zurück, inklusive aller Nachrichten.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{\"conversationId\":{\"type\":\"string\",\"description\":\"UUID der Konversation\"}},\"required\":[\"conversationId\"]}";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            String conversationIdStr = inputNode.get("conversationId").asText();
            UUID conversationId = UUID.fromString(conversationIdStr);

            ConversationEntity conversation = conversationService.findById(conversationId);
            Page<ConversationMessageEntity> messagesPage = messageService.getMessages(conversationId, Pageable.unpaged());

            List<Map<String, Object>> messages = messagesPage.getContent().stream()
                    .map(msg -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", msg.getId().toString());
                        m.put("content", msg.getContent());
                        m.put("senderType", msg.getSenderType().name());
                        m.put("senderId", msg.getSenderId() != null ? msg.getSenderId().toString() : null);
                        m.put("sentAt", msg.getSentAt().toString());
                        return m;
                    })
                    .toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", conversation.getId().toString());
            result.put("subject", conversation.getSubject());
            result.put("status", conversation.getStatus().name());
            result.put("priority", conversation.getPriority().name());
            result.put("source", conversation.getSource().name());
            result.put("customerId", conversation.getCustomerId() != null ? conversation.getCustomerId().toString() : null);
            result.put("assignedTo", conversation.getAssignedTo() != null ? conversation.getAssignedTo().toString() : null);
            result.put("slaDueAt", conversation.getSlaDueAt() != null ? conversation.getSlaDueAt().toString() : null);
            result.put("messageCount", messages.size());
            result.put("messages", messages);

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_conversation_detail: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Konversationsdetails: " + e.getMessage());
        }
    }
}
