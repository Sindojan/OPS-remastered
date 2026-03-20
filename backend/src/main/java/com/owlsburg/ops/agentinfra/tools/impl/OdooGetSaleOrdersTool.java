package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.odoo.OdooProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OdooGetSaleOrdersTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(OdooGetSaleOrdersTool.class);

    private final OdooProxyService odooProxyService;
    private final ObjectMapper objectMapper;

    public OdooGetSaleOrdersTool(OdooProxyService odooProxyService, ObjectMapper objectMapper) {
        this.odooProxyService = odooProxyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "odoo_get_sale_orders";
    }

    @Override
    public String getDescription() {
        return "Verkaufsaufträge aus Odoo abrufen (sale.order).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "state":{"type":"string","description":"Status-Filter (z.B. sale, draft, done, cancel)"},
              "limit":{"type":"integer","description":"Maximale Anzahl (default: 20)"}
            }}""";
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
            String state = inputNode.has("state") ? inputNode.get("state").asText() : null;
            int limit = inputNode.has("limit") ? inputNode.get("limit").asInt(20) : 20;

            UUID tenantId = UUID.fromString(context.tenantId());
            String result = odooProxyService.getSaleOrders(tenantId, state, limit);
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Error executing odoo_get_sale_orders: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Verkaufsaufträge aus Odoo: " + e.getMessage());
        }
    }
}
