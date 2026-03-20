package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.knowledge.KnowledgeArticleService;
import com.owlsburg.ops.knowledge.dto.KnowledgeArticleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class GetKnowledgeArticleTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetKnowledgeArticleTool.class);

    private final KnowledgeArticleService knowledgeArticleService;
    private final ObjectMapper objectMapper;

    public GetKnowledgeArticleTool(KnowledgeArticleService knowledgeArticleService, ObjectMapper objectMapper) {
        this.knowledgeArticleService = knowledgeArticleService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_knowledge_article";
    }

    @Override
    public String getDescription() {
        return "Einen Wissensartikel mit vollständigem Inhalt, Kategorie und Tags lesen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "articleId":{"type":"string","description":"UUID des Wissensartikels"}
            },"required":["articleId"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public String getModuleId() {
        return "knowledge";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            UUID articleId = UUID.fromString(inputNode.get("articleId").asText());

            KnowledgeArticleResponse article = knowledgeArticleService.findByIdAsResponse(articleId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", article.id());
            result.put("titel", article.title());
            result.put("inhalt", article.content());
            result.put("auszug", article.excerpt());
            result.put("status", article.status());
            result.put("kategorie", article.categoryName());
            result.put("autorId", article.authorId() != null ? article.authorId().toString() : null);
            result.put("tags", article.tags() != null ? article.tags().stream().map(t -> t.name()).toList() : null);
            result.put("veröffentlichtAm", article.publishedAt());
            result.put("aktualisiertAm", article.updatedAt());

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_knowledge_article: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden des Wissensartikels: " + e.getMessage());
        }
    }
}
