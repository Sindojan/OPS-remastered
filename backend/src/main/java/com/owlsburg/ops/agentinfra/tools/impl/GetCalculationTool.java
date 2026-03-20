package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.bom.CalculationEntity;
import com.owlsburg.ops.bom.CalculationService;
import com.owlsburg.ops.bom.PartEntity;
import com.owlsburg.ops.bom.PartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetCalculationTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetCalculationTool.class);

    private final CalculationService calculationService;
    private final PartService partService;
    private final ObjectMapper objectMapper;

    public GetCalculationTool(CalculationService calculationService, PartService partService, ObjectMapper objectMapper) {
        this.calculationService = calculationService;
        this.partService = partService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_calculation";
    }

    @Override
    public String getDescription() {
        return "Kalkulationshistorie eines Teils abrufen (Material-, Lohn-, Gemeinkosten, Gesamtkosten).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "partId":{"type":"string","description":"UUID des Teils"},
              "limit":{"type":"integer","description":"Maximale Anzahl Kalkulationen (default: 5)"}
            },"required":["partId"]}""";
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
            UUID partId = UUID.fromString(inputNode.get("partId").asText());
            int limit = inputNode.has("limit") ? inputNode.get("limit").asInt(5) : 5;

            PartEntity part = partService.getById(partId);
            List<CalculationEntity> history = calculationService.getHistoryByPart(partId);

            List<Map<String, Object>> calcs = history.stream().limit(limit).map(c -> {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("id", c.getId().toString());
                cm.put("menge", c.getQuantity());
                cm.put("materialkosten", c.getMaterialCost());
                cm.put("lohnkosten", c.getLaborCost());
                cm.put("gemeinkosten", c.getOverheadCost());
                cm.put("gesamtkosten", c.getTotalCost());
                cm.put("währung", c.getCurrency());
                cm.put("kalkuliertAm", c.getCalculatedAt() != null ? c.getCalculatedAt().toString() : null);
                return cm;
            }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("teil", part.getPartNumber() + " – " + part.getName());
            result.put("kalkulationen", calcs);
            result.put("gesamt", history.size());
            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_calculation: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Kalkulation: " + e.getMessage());
        }
    }
}
