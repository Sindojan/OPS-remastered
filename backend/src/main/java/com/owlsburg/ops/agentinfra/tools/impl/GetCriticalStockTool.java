package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.inventory.ArticleService;
import com.owlsburg.ops.inventory.ArticleEntity;
import com.owlsburg.ops.inventory.StockService;
import com.owlsburg.ops.inventory.dto.CriticalArticleResponse;
import com.owlsburg.ops.inventory.dto.StockSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetCriticalStockTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetCriticalStockTool.class);

    private final StockService stockService;
    private final ArticleService articleService;
    private final ObjectMapper objectMapper;

    public GetCriticalStockTool(StockService stockService,
                                ArticleService articleService,
                                ObjectMapper objectMapper) {
        this.stockService = stockService;
        this.articleService = articleService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_critical_stock";
    }

    @Override
    public String getDescription() {
        return "Artikel unter Mindestbestand oder Nachbestellpunkt anzeigen. KRITISCH = unter Mindestbestand, WARNUNG = unter Nachbestellpunkt aber über Mindestbestand.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "severity":{"type":"string","enum":["KRITISCH","WARNUNG"],"description":"Schweregrad-Filter"}
            }}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            String severityFilter = inputNode.has("severity") ? inputNode.get("severity").asText() : null;

            List<CriticalArticleResponse> criticalArticles = stockService.getCriticalArticles();

            List<Map<String, Object>> result = criticalArticles.stream()
                    .map(article -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("artikelId", article.articleId().toString());

                        // Try to get article details
                        try {
                            ArticleEntity entity = articleService.getArticleById(article.articleId());
                            m.put("artikelNummer", entity.getArticleNumber());
                            m.put("name", entity.getName());
                        } catch (Exception e) {
                            m.put("artikelNummer", "unbekannt");
                            m.put("name", "unbekannt");
                        }

                        m.put("aktuellerBestand", article.currentQuantity());
                        m.put("mindestbestand", article.minStock());
                        m.put("fehlendeMenge", article.deficit());

                        // Determine severity
                        boolean isCritical = article.currentQuantity().compareTo(article.minStock()) < 0;
                        m.put("schweregrad", isCritical ? "KRITISCH" : "WARNUNG");

                        return m;
                    })
                    .filter(m -> {
                        if (severityFilter == null) return true;
                        return severityFilter.equals(m.get("schweregrad"));
                    })
                    .toList();

            String json = objectMapper.writeValueAsString(Map.of(
                    "anzahl", result.size(),
                    "artikel", result
            ));
            if (json.length() > 2000) {
                json = json.substring(0, 1950) + "...\n[Ergebnis gekürzt]";
            }
            return ToolResult.success(json);
        } catch (Exception e) {
            log.error("Error executing get_critical_stock: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden des kritischen Bestands: " + e.getMessage());
        }
    }
}
