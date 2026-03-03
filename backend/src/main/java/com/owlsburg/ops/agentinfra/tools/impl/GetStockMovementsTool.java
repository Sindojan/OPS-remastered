package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.inventory.StockMovementEntity;
import com.owlsburg.ops.inventory.StockMovementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetStockMovementsTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetStockMovementsTool.class);

    private final StockMovementService stockMovementService;
    private final ObjectMapper objectMapper;

    public GetStockMovementsTool(StockMovementService stockMovementService, ObjectMapper objectMapper) {
        this.stockMovementService = stockMovementService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_stock_movements";
    }

    @Override
    public String getDescription() {
        return "Letzte Lagerbewegungen abrufen, optional nach Artikel gefiltert.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "articleId":{"type":"string","description":"UUID des Artikels (optional, für Filter)"},
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
            PageRequest pageable = PageRequest.of(0, Math.min(limit, 50));

            Page<StockMovementEntity> movements;
            if (node.has("articleId") && !node.get("articleId").isNull()) {
                UUID articleId = UUID.fromString(node.get("articleId").asText());
                movements = stockMovementService.findByArticle(articleId, pageable);
            } else {
                movements = stockMovementService.findRecentMovements(pageable);
            }

            List<Map<String, Object>> result = movements.getContent().stream()
                    .map(m -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", m.getId().toString());
                        map.put("artikelId", m.getArticleId().toString());
                        map.put("typ", m.getType().name());
                        map.put("menge", m.getQuantity());
                        map.put("notizen", m.getNotes());
                        map.put("datum", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
                        return map;
                    })
                    .toList();

            // Limit entries to avoid JSON truncation at byte boundary
            List<Map<String, Object>> truncated = result.size() > 15 ? result.subList(0, 15) : result;
            String json = objectMapper.writeValueAsString(Map.of(
                    "anzahl", truncated.size(),
                    "gesamt", movements.getTotalElements(),
                    "bewegungen", truncated
            ));
            return ToolResult.success(json);
        } catch (Exception e) {
            log.error("Error executing get_stock_movements: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Lagerbewegungen: " + e.getMessage());
        }
    }

}
