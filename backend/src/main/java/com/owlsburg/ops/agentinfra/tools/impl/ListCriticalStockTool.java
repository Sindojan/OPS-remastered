package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.inventory.StockService;
import com.owlsburg.ops.inventory.dto.CriticalArticleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ListCriticalStockTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ListCriticalStockTool.class);

    private final StockService stockService;
    private final ObjectMapper objectMapper;

    public ListCriticalStockTool(StockService stockService, ObjectMapper objectMapper) {
        this.stockService = stockService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_stock_summary";
    }

    @Override
    public String getDescription() {
        return "Zeigt alle Artikel mit kritischem Bestand an – Artikel, deren Lagerbestand unter dem Mindestbestand liegt.";
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
    public String getModuleId() {
        return "inventory";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            List<CriticalArticleResponse> criticalArticles = stockService.getCriticalArticles();

            List<Map<String, Object>> result = criticalArticles.stream()
                    .map(article -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("articleId", article.articleId().toString());
                        m.put("warehouseLocationId", article.warehouseLocationId() != null ? article.warehouseLocationId().toString() : null);
                        m.put("currentQuantity", article.currentQuantity());
                        m.put("minStock", article.minStock());
                        m.put("deficit", article.deficit());
                        return m;
                    })
                    .toList();

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_stock_summary: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden des kritischen Bestands: " + e.getMessage());
        }
    }
}
