package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.inventory.ArticleEntity;
import com.owlsburg.ops.inventory.ArticleService;
import com.owlsburg.ops.inventory.StockService;
import com.owlsburg.ops.inventory.dto.StockSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class GetStockLevelTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetStockLevelTool.class);

    private final StockService stockService;
    private final ArticleService articleService;
    private final ObjectMapper objectMapper;

    public GetStockLevelTool(StockService stockService,
                             ArticleService articleService,
                             ObjectMapper objectMapper) {
        this.stockService = stockService;
        this.articleService = articleService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_stock_level";
    }

    @Override
    public String getDescription() {
        return "Bestand eines bestimmten Artikels abrufen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "articleId":{"type":"string","description":"UUID des Artikels"}
            },"required":["articleId"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public String getModuleId() {
        return "inventory";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            UUID articleId = UUID.fromString(inputNode.get("articleId").asText());

            ArticleEntity article = articleService.getArticleById(articleId);
            StockSummaryResponse stock = stockService.getStock(articleId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("artikelId", articleId.toString());
            result.put("artikelNummer", article.getArticleNumber());
            result.put("name", article.getName());
            result.put("bestand", stock.totalQuantity());
            result.put("reserviert", stock.totalReserved());
            result.put("verfügbar", stock.totalAvailable());

            // Determine status
            String status;
            if (article.getMinStock() != null && stock.totalAvailable().compareTo(article.getMinStock()) < 0) {
                status = "KRITISCH";
            } else if (article.getReorderPoint() != null && stock.totalAvailable().compareTo(article.getReorderPoint()) < 0) {
                status = "WARNUNG";
            } else {
                status = "OK";
            }
            result.put("status", status);

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_stock_level: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden des Bestands: " + e.getMessage());
        }
    }
}
