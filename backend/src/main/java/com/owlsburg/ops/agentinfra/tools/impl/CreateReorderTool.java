package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.inventory.ReorderRequestEntity;
import com.owlsburg.ops.inventory.ReorderRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class CreateReorderTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(CreateReorderTool.class);

    private final ReorderRequestService reorderRequestService;
    private final ObjectMapper objectMapper;

    public CreateReorderTool(ReorderRequestService reorderRequestService, ObjectMapper objectMapper) {
        this.reorderRequestService = reorderRequestService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "create_reorder";
    }

    @Override
    public String getDescription() {
        return "Erstellt eine Nachbestellungsanfrage für einen Artikel.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "articleId":{"type":"string","description":"UUID des Artikels"},
              "quantity":{"type":"number","description":"Bestellmenge"},
              "supplierId":{"type":"string","description":"UUID des Lieferanten (optional)"},
              "notes":{"type":"string","description":"Anmerkungen (optional)"}
            },"required":["articleId","quantity"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public String getModuleId() {
        return "inventory";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            UUID articleId = UUID.fromString(node.get("articleId").asText());
            BigDecimal quantity = new BigDecimal(node.get("quantity").asText());
            UUID supplierId = node.has("supplierId") && !node.get("supplierId").isNull()
                    ? UUID.fromString(node.get("supplierId").asText()) : null;
            String notes = node.has("notes") ? node.get("notes").asText() : null;

            ReorderRequestEntity entity = reorderRequestService.create(
                    articleId, quantity, supplierId, notes, "Lager-Lead Agent");

            return ToolResult.success(objectMapper.writeValueAsString(Map.of(
                    "status", "erstellt",
                    "reorderId", entity.getId().toString(),
                    "artikelId", articleId.toString(),
                    "menge", quantity
            )));
        } catch (Exception e) {
            log.error("Error executing create_reorder: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Erstellen der Nachbestellung: " + e.getMessage());
        }
    }
}
