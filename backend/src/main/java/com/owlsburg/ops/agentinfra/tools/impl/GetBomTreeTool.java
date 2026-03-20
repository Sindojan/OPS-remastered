package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.bom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetBomTreeTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetBomTreeTool.class);

    private final BomService bomService;
    private final PartService partService;
    private final ObjectMapper objectMapper;

    public GetBomTreeTool(BomService bomService, PartService partService, ObjectMapper objectMapper) {
        this.bomService = bomService;
        this.partService = partService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_bom_tree";
    }

    @Override
    public String getDescription() {
        return "Aktive Stückliste (BOM) eines Teils mit allen Komponenten und Mengen abrufen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "partId":{"type":"string","description":"UUID des Teils"}
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

            PartEntity part = partService.getById(partId);
            BomVersionEntity activeVersion = bomService.getActiveVersionForPart(partId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("teil", part.getPartNumber() + " – " + part.getName());
            result.put("typ", part.getType());
            result.put("bomVersion", activeVersion.getVersionNumber());
            result.put("bomStatus", activeVersion.getStatus().name());

            List<BomItemEntity> items = bomService.getItems(activeVersion.getId());
            result.put("komponenten", items.stream().map(item -> {
                Map<String, Object> im = new LinkedHashMap<>();
                PartEntity component = partService.getById(item.getComponentPartId());
                im.put("position", item.getPosition());
                im.put("teilenummer", component.getPartNumber());
                im.put("name", component.getName());
                im.put("menge", item.getQuantity());
                im.put("notizen", item.getNotes());
                return im;
            }).toList());
            result.put("anzahlKomponenten", items.size());

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_bom_tree: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Stückliste: " + e.getMessage());
        }
    }
}
