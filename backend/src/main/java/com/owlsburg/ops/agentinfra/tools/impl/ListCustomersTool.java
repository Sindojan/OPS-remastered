package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.customers.CustomerEntity;
import com.owlsburg.ops.customers.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ListCustomersTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ListCustomersTool.class);

    private final CustomerService customerService;
    private final ObjectMapper objectMapper;

    public ListCustomersTool(CustomerService customerService, ObjectMapper objectMapper) {
        this.customerService = customerService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "list_customers";
    }

    @Override
    public String getDescription() {
        return "Kundenliste mit Firmenname, Kundennummer und Status abrufen.";
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
        return "customers";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            int limit = inputNode.has("limit") ? inputNode.get("limit").asInt(20) : 20;

            Page<CustomerEntity> page = customerService.findAll(PageRequest.of(0, limit));
            List<Map<String, Object>> customers = page.getContent().stream().map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId().toString());
                m.put("firmenname", c.getCompanyName());
                m.put("kundennummer", c.getCustomerNumber());
                m.put("kurzname", c.getShortName());
                m.put("status", c.getStatus());
                m.put("kontakte", c.getContacts() != null ? c.getContacts().size() : 0);
                return m;
            }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("kunden", customers);
            result.put("gesamt", page.getTotalElements());
            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing list_customers: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Kundenliste: " + e.getMessage());
        }
    }
}
