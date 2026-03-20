package com.owlsburg.ops.odoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OdooProxyService {

    private static final Logger log = LoggerFactory.getLogger(OdooProxyService.class);

    private final OdooConfigService configService;
    private final OdooClient odooClient;
    private final ObjectMapper objectMapper;

    public OdooProxyService(OdooConfigService configService,
                            OdooClient odooClient,
                            ObjectMapper objectMapper) {
        this.configService = configService;
        this.odooClient = odooClient;
        this.objectMapper = objectMapper;
    }

    public String searchPartners(UUID tenantId, String query, String type, int limit) {
        var conn = configService.getConnectionDetails(tenantId);
        List<Object> domain = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            domain.add(List.of("name", "ilike", "%" + query + "%"));
        }
        if ("customer".equalsIgnoreCase(type)) {
            domain.add(List.of("customer_rank", ">", 0));
        } else if ("supplier".equalsIgnoreCase(type)) {
            domain.add(List.of("supplier_rank", ">", 0));
        }

        List<String> fields = List.of("name", "email", "phone", "city", "is_company", "customer_rank", "supplier_rank");
        return toJson(odooClient.searchRead(conn.baseUrl(), conn.database(), conn.apiKey(),
                "res.partner", domain, fields, limit > 0 ? limit : 20));
    }

    public String searchProducts(UUID tenantId, String query, int limit) {
        var conn = configService.getConnectionDetails(tenantId);
        List<Object> domain = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            domain.add(List.of("|"));
            domain.add(List.of("name", "ilike", "%" + query + "%"));
            domain.add(List.of("default_code", "ilike", "%" + query + "%"));
        }

        List<String> fields = List.of("name", "default_code", "list_price", "qty_available", "categ_id");
        return toJson(odooClient.searchRead(conn.baseUrl(), conn.database(), conn.apiKey(),
                "product.product", domain, fields, limit > 0 ? limit : 20));
    }

    public String getSaleOrders(UUID tenantId, String state, int limit) {
        var conn = configService.getConnectionDetails(tenantId);
        List<Object> domain = new ArrayList<>();
        if (state != null && !state.isBlank()) {
            domain.add(List.of("state", "=", state));
        }

        List<String> fields = List.of("name", "partner_id", "date_order", "state", "amount_total");
        return toJson(odooClient.searchRead(conn.baseUrl(), conn.database(), conn.apiKey(),
                "sale.order", domain, fields, limit > 0 ? limit : 20));
    }

    public String getPurchaseOrders(UUID tenantId, String state, int limit) {
        var conn = configService.getConnectionDetails(tenantId);
        List<Object> domain = new ArrayList<>();
        if (state != null && !state.isBlank()) {
            domain.add(List.of("state", "=", state));
        }

        List<String> fields = List.of("name", "partner_id", "date_order", "state", "amount_total");
        return toJson(odooClient.searchRead(conn.baseUrl(), conn.database(), conn.apiKey(),
                "purchase.order", domain, fields, limit > 0 ? limit : 20));
    }

    public String getStockQuants(UUID tenantId, String productQuery, int limit) {
        var conn = configService.getConnectionDetails(tenantId);
        List<Object> domain = new ArrayList<>();
        if (productQuery != null && !productQuery.isBlank()) {
            domain.add(List.of("product_id.name", "ilike", "%" + productQuery + "%"));
        }

        List<String> fields = List.of("product_id", "location_id", "quantity", "reserved_quantity");
        return toJson(odooClient.searchRead(conn.baseUrl(), conn.database(), conn.apiKey(),
                "stock.quant", domain, fields, limit > 0 ? limit : 20));
    }

    public String getManufacturingOrders(UUID tenantId, String state, int limit) {
        var conn = configService.getConnectionDetails(tenantId);
        List<Object> domain = new ArrayList<>();
        if (state != null && !state.isBlank()) {
            domain.add(List.of("state", "=", state));
        }

        List<String> fields = List.of("name", "product_id", "product_qty", "state", "date_start");
        try {
            return toJson(odooClient.searchRead(conn.baseUrl(), conn.database(), conn.apiKey(),
                    "mrp.production", domain, fields, limit > 0 ? limit : 20));
        } catch (OdooApiException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return "{\"error\": \"Das MRP-Modul (Fertigung) ist in dieser Odoo-Instanz nicht installiert.\"}";
            }
            throw e;
        }
    }

    public String getEmployees(UUID tenantId, String department, int limit) {
        var conn = configService.getConnectionDetails(tenantId);
        List<Object> domain = new ArrayList<>();
        if (department != null && !department.isBlank()) {
            domain.add(List.of("department_id.name", "ilike", "%" + department + "%"));
        }

        List<String> fields = List.of("name", "job_title", "department_id", "work_email", "work_phone");
        try {
            return toJson(odooClient.searchRead(conn.baseUrl(), conn.database(), conn.apiKey(),
                    "hr.employee", domain, fields, limit > 0 ? limit : 20));
        } catch (OdooApiException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return "{\"error\": \"Das HR-Modul (Personal) ist in dieser Odoo-Instanz nicht installiert.\"}";
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public String genericSearch(UUID tenantId, String model, Object domain, List<String> fields, int limit) {
        var conn = configService.getConnectionDetails(tenantId);
        List<Object> domainList;
        if (domain instanceof List) {
            domainList = (List<Object>) domain;
        } else {
            domainList = List.of();
        }

        return toJson(odooClient.searchRead(conn.baseUrl(), conn.database(), conn.apiKey(),
                model, domainList, fields, limit > 0 ? limit : 20));
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.error("Failed to serialize Odoo response: {}", e.getMessage());
            return node.toString();
        }
    }
}
