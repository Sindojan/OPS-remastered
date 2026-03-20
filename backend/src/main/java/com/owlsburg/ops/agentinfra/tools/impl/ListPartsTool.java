package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.bom.PartEntity;
import com.owlsburg.ops.bom.PartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ListPartsTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ListPartsTool.class);

    private final PartService partService;
    private final ObjectMapper objectMapper;

    public ListPartsTool(PartService partService, ObjectMapper objectMapper) {
        this.partService = partService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "list_parts";
    }

    @Override
    public String getDescription() {
        return "Teile und Komponenten auflisten (Teilenummer, Name, Typ).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "limit":{"type":"integer","description":"Maximale Anzahl (default: 20)"}
            }}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public String getModuleId() {
        return "bom";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            int limit = inputNode.has("limit") ? inputNode.get("limit").asInt(20) : 20;

            Page<PartEntity> page = partService.findAll(PageRequest.of(0, limit));
            List<Map<String, Object>> parts = page.getContent().stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId().toString());
                m.put("teilenummer", p.getPartNumber());
                m.put("name", p.getName());
                m.put("typ", p.getType());
                m.put("status", p.getStatus());
                return m;
            }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("teile", parts);
            result.put("gesamt", page.getTotalElements());
            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing list_parts: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Teileliste: " + e.getMessage());
        }
    }
}
