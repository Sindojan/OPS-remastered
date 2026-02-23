package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.inbox.ConversationEntity;
import com.owlsburg.ops.inbox.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ListConversationsTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ListConversationsTool.class);

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public ListConversationsTool(ConversationService conversationService, ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_open_conversations";
    }

    @Override
    public String getDescription() {
        return "Listet alle offenen Konversationen/Support-Tickets auf.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            Page<ConversationEntity> conversations = conversationService.findAll(Pageable.unpaged(), "OPEN");

            List<Map<String, Object>> result = conversations.getContent().stream()
                    .map(conv -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", conv.getId().toString());
                        m.put("subject", conv.getSubject());
                        m.put("status", conv.getStatus().name());
                        m.put("priority", conv.getPriority().name());
                        m.put("source", conv.getSource().name());
                        m.put("customerId", conv.getCustomerId() != null ? conv.getCustomerId().toString() : null);
                        m.put("assignedTo", conv.getAssignedTo() != null ? conv.getAssignedTo().toString() : null);
                        m.put("slaDueAt", conv.getSlaDueAt() != null ? conv.getSlaDueAt().toString() : null);
                        m.put("createdAt", conv.getCreatedAt() != null ? conv.getCreatedAt().toString() : null);
                        return m;
                    })
                    .toList();

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_open_conversations: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der offenen Konversationen: " + e.getMessage());
        }
    }
}
