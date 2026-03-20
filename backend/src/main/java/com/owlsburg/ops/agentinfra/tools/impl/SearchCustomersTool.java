package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.customers.CustomerEntity;
import com.owlsburg.ops.customers.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SearchCustomersTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(SearchCustomersTool.class);

    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    public SearchCustomersTool(CustomerRepository customerRepository, ObjectMapper objectMapper) {
        this.customerRepository = customerRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "search_customers";
    }

    @Override
    public String getDescription() {
        return "Kunden nach Firmenname oder Kundennummer suchen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "query":{"type":"string","description":"Suchbegriff (Firmenname oder Kundennummer)"},
              "status":{"type":"string","description":"Status-Filter (ACTIVE, INACTIVE)"},
              "limit":{"type":"integer","description":"Maximale Anzahl (default: 10)"}
            },"required":["query"]}""";
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
            String query = inputNode.get("query").asText();
            String status = inputNode.has("status") ? inputNode.get("status").asText() : null;
            int limit = inputNode.has("limit") ? inputNode.get("limit").asInt(10) : 10;

            Page<CustomerEntity> page;
            if (status != null && !status.isBlank()) {
                page = customerRepository.findByStatus(status, PageRequest.of(0, limit));
            } else {
                page = customerRepository.findAll(PageRequest.of(0, limit));
            }

            // Filter by query in memory (name or number)
            String lowerQuery = query.toLowerCase();
            List<Map<String, Object>> matches = page.getContent().stream()
                    .filter(c -> (c.getCompanyName() != null && c.getCompanyName().toLowerCase().contains(lowerQuery))
                            || (c.getCustomerNumber() != null && c.getCustomerNumber().toLowerCase().contains(lowerQuery))
                            || (c.getShortName() != null && c.getShortName().toLowerCase().contains(lowerQuery)))
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", c.getId().toString());
                        m.put("firmenname", c.getCompanyName());
                        m.put("kundennummer", c.getCustomerNumber());
                        m.put("kurzname", c.getShortName());
                        m.put("status", c.getStatus());
                        return m;
                    }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("treffer", matches);
            result.put("anzahl", matches.size());
            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing search_customers: {}", e.getMessage(), e);
            return ToolResult.error("Fehler bei der Kundensuche: " + e.getMessage());
        }
    }
}
