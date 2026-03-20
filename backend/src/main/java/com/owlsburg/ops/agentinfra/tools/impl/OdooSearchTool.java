package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.odoo.OdooProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class OdooSearchTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(OdooSearchTool.class);

    private final OdooProxyService odooProxyService;
    private final ObjectMapper objectMapper;

    public OdooSearchTool(OdooProxyService odooProxyService, ObjectMapper objectMapper) {
        this.odooProxyService = odooProxyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "odoo_search";
    }

    @Override
    public String getDescription() {
        return "Generische Suche auf beliebigem Odoo-Modell (z.B. res.partner, sale.order, product.product).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "model":{"type":"string","description":"Odoo Model (z.B. res.partner, sale.order, product.product)"},
              "domain":{"type":"array","description":"Odoo Domain-Filter als Liste von Tripeln, z.B. [[\"name\",\"ilike\",\"test\"]]"},
              "fields":{"type":"array","items":{"type":"string"},"description":"Felder die zurückgegeben werden sollen"},
              "limit":{"type":"integer","description":"Maximale Anzahl Ergebnisse (default: 20)"}
            },"required":["model"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public String getModuleId() {
        return "odoo";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            String model = inputNode.get("model").asText();
            Object domain = inputNode.has("domain") ? objectMapper.treeToValue(inputNode.get("domain"), Object.class) : null;

            List<String> fields = new ArrayList<>();
            if (inputNode.has("fields") && inputNode.get("fields").isArray()) {
                for (JsonNode f : inputNode.get("fields")) {
                    fields.add(f.asText());
                }
            }

            int limit = inputNode.has("limit") ? inputNode.get("limit").asInt(20) : 20;

            UUID tenantId = UUID.fromString(context.tenantId());
            String result = odooProxyService.genericSearch(tenantId, model, domain, fields.isEmpty() ? null : fields, limit);
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error executing odoo_search: {}", e.getMessage(), e);
            return ToolResult.error("Fehler bei Odoo-Suche: " + e.getMessage());
        }
    }
}
