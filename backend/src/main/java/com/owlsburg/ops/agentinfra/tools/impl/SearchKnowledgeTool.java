package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.knowledge.KnowledgeSearchService;
import com.owlsburg.ops.knowledge.dto.KnowledgeSearchResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SearchKnowledgeTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(SearchKnowledgeTool.class);

    private final KnowledgeSearchService knowledgeSearchService;
    private final ObjectMapper objectMapper;

    public SearchKnowledgeTool(KnowledgeSearchService knowledgeSearchService, ObjectMapper objectMapper) {
        this.knowledgeSearchService = knowledgeSearchService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "search_knowledge";
    }

    @Override
    public String getDescription() {
        return "Wissensdatenbank durchsuchen (Artikel und Dokumente).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "query":{"type":"string","description":"Suchbegriff"}
            },"required":["query"]}""";
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
            String query = inputNode.get("query").asText();

            List<KnowledgeSearchResultResponse> results = knowledgeSearchService.search(query);

            List<Map<String, Object>> hits = results.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("typ", r.type());
                m.put("id", r.id());
                m.put("titel", r.title());
                m.put("auszug", r.excerpt());
                m.put("kategorie", r.category());
                m.put("aktualisiertAm", r.updatedAt() != null ? r.updatedAt().toString() : null);
                return m;
            }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("treffer", hits);
            result.put("anzahl", hits.size());
            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing search_knowledge: {}", e.getMessage(), e);
            return ToolResult.error("Fehler bei der Wissenssuche: " + e.getMessage());
        }
    }
}
