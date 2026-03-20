package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.customers.CustomerEntity;
import com.owlsburg.ops.customers.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class GetCustomerDetailTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetCustomerDetailTool.class);

    private final CustomerService customerService;
    private final ObjectMapper objectMapper;

    public GetCustomerDetailTool(CustomerService customerService, ObjectMapper objectMapper) {
        this.customerService = customerService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_customer_detail";
    }

    @Override
    public String getDescription() {
        return "Detailansicht eines Kunden mit Kontakten, Adressen und Preisgruppen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "customerId":{"type":"string","description":"UUID des Kunden"}
            },"required":["customerId"]}""";
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
            UUID customerId = UUID.fromString(inputNode.get("customerId").asText());

            CustomerEntity customer = customerService.findById(customerId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", customer.getId().toString());
            result.put("firmenname", customer.getCompanyName());
            result.put("kundennummer", customer.getCustomerNumber());
            result.put("kurzname", customer.getShortName());
            result.put("steuerId", customer.getTaxId());
            result.put("status", customer.getStatus());

            if (customer.getContacts() != null) {
                result.put("kontakte", customer.getContacts().stream().map(c -> {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("name", c.getFirstName() + " " + c.getLastName());
                    cm.put("email", c.getEmail());
                    cm.put("telefon", c.getPhone());
                    cm.put("position", c.getPosition());
                    cm.put("hauptkontakt", c.isPrimary());
                    return cm;
                }).toList());
            }

            if (customer.getAddresses() != null) {
                result.put("adressen", customer.getAddresses().stream().map(a -> {
                    Map<String, Object> am = new LinkedHashMap<>();
                    am.put("typ", a.getType().name());
                    am.put("strasse", a.getStreet());
                    am.put("plz", a.getZip());
                    am.put("stadt", a.getCity());
                    am.put("land", a.getCountry());
                    return am;
                }).toList());
            }

            if (customer.getPriceGroups() != null) {
                result.put("preisgruppen", customer.getPriceGroups().stream().map(pg -> {
                    Map<String, Object> pgm = new LinkedHashMap<>();
                    pgm.put("name", pg.getName());
                    pgm.put("rabattProzent", pg.getDiscountPercent());
                    pgm.put("gültigVon", pg.getValidFrom() != null ? pg.getValidFrom().toString() : null);
                    pgm.put("gültigBis", pg.getValidUntil() != null ? pg.getValidUntil().toString() : null);
                    return pgm;
                }).toList());
            }

            return ToolResult.success(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Error executing get_customer_detail: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Kundendetails: " + e.getMessage());
        }
    }
}
