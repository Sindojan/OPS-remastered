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
public class GetArticleDetailTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetArticleDetailTool.class);

    private final ArticleService articleService;
    private final StockService stockService;
    private final ObjectMapper objectMapper;

    public GetArticleDetailTool(ArticleService articleService, StockService stockService, ObjectMapper objectMapper) {
        this.articleService = articleService;
        this.stockService = stockService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_article_detail";
    }

    @Override
    public String getDescription() {
        return "Gibt die Details eines Artikels zurück, inklusive Bestandsinformationen pro Lagerort.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{\"articleId\":{\"type\":\"string\",\"description\":\"UUID des Artikels\"}},\"required\":[\"articleId\"]}";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            String articleIdStr = inputNode.get("articleId").asText();
            UUID articleId = UUID.fromString(articleIdStr);

            ArticleEntity article = articleService.getArticleById(articleId);
            StockSummaryResponse stockSummary = stockService.getStock(articleId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", article.getId().toString());
            result.put("articleNumber", article.getArticleNumber());
            result.put("name", article.getName());
            result.put("description", article.getDescription());
            result.put("categoryId", article.getCategoryId() != null ? article.getCategoryId().toString() : null);
            result.put("unitId", article.getUnitId() != null ? article.getUnitId().toString() : null);
            result.put("minStock", article.getMinStock());
            result.put("reorderPoint", article.getReorderPoint());
            result.put("status", article.getStatus());
            result.put("totalQuantity", stockSummary.totalQuantity());
            result.put("totalReserved", stockSummary.totalReserved());
            result.put("totalAvailable", stockSummary.totalAvailable());
            result.put("locationCount", stockSummary.locations().size());

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_article_detail: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Artikeldetails: " + e.getMessage());
        }
    }
}
