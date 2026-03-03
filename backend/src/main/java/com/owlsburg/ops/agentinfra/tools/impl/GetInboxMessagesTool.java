package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.inbox.ConversationEntity;
import com.owlsburg.ops.inbox.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetInboxMessagesTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetInboxMessagesTool.class);

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public GetInboxMessagesTool(ConversationService conversationService, ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_inbox_messages";
    }

    @Override
    public String getDescription() {
        return "Konversationen aus dem Posteingang abrufen, optional nach Status gefiltert.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "status":{"type":"string","enum":["OPEN","IN_PROGRESS","RESOLVED","CLOSED"],"description":"Filter nach Status"},
              "limit":{"type":"integer","description":"Maximale Anzahl (Standard: 20)"}
            }}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            int limit = node.has("limit") ? node.get("limit").asInt(20) : 20;
            String status = node.has("status") && !node.get("status").isNull()
                    ? node.get("status").asText() : null;

            Page<ConversationEntity> conversations = conversationService.findAll(
                    PageRequest.of(0, Math.min(limit, 50)), status);

            List<Map<String, Object>> result = conversations.getContent().stream()
                    .map(c -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", c.getId().toString());
                        map.put("betreff", c.getSubject());
                        map.put("status", c.getStatus().name());
                        map.put("priorität", c.getPriority().name());
                        map.put("quelle", c.getSource().name());
                        map.put("erstellt", c.getCreatedAt().toString());
                        return map;
                    })
                    .toList();

            List<Map<String, Object>> truncated = result.size() > 15 ? result.subList(0, 15) : result;
            String json = objectMapper.writeValueAsString(Map.of(
                    "anzahl", truncated.size(),
                    "gesamt", conversations.getTotalElements(),
                    "konversationen", truncated
            ));
            return ToolResult.success(json);
        } catch (Exception e) {
            log.error("Error executing get_inbox_messages: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Konversationen: " + e.getMessage());
        }
    }

}
