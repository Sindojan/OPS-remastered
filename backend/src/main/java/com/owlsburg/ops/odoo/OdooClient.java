package com.owlsburg.ops.odoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OdooClient {

    private static final Logger log = LoggerFactory.getLogger(OdooClient.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper;

    public OdooClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Core method: Call Odoo JSON-2 API.
     * POST {baseUrl}/json/2/{model}/{method}
     */
    public JsonNode call(String baseUrl, String database, String apiKey,
                         String model, String method, Map<String, Object> params) {
        try {
            String url = normalizeBaseUrl(baseUrl) + "/json/2/" + model + "/" + method;

            ObjectNode body = objectMapper.createObjectNode();
            if (params != null) {
                body = objectMapper.valueToTree(params);
            }

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-Odoo-Database", database)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.debug("Odoo API call: {} {}", method, model);

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OdooApiException("Odoo API returned HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode responseNode = objectMapper.readTree(response.body());

            if (responseNode.has("error")) {
                JsonNode error = responseNode.get("error");
                String errorMessage = error.has("message") ? error.get("message").asText() : error.toString();
                throw new OdooApiException("Odoo API error: " + errorMessage);
            }

            return responseNode.has("result") ? responseNode.get("result") : responseNode;
        } catch (OdooApiException e) {
            throw e;
        } catch (Exception e) {
            throw new OdooApiException("Odoo API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * search_read: Search and read records.
     */
    public JsonNode searchRead(String baseUrl, String database, String apiKey,
                                String model, List<Object> domain, List<String> fields, int limit) {
        Map<String, Object> params = Map.of(
                "domain", domain != null ? domain : List.of(),
                "fields", fields != null ? fields : List.of(),
                "limit", limit
        );
        return call(baseUrl, database, apiKey, model, "search_read", params);
    }

    /**
     * search_count: Count matching records.
     */
    public int searchCount(String baseUrl, String database, String apiKey,
                           String model, List<Object> domain) {
        JsonNode result = call(baseUrl, database, apiKey, model, "search_count",
                Map.of("domain", domain != null ? domain : List.of()));
        return result.asInt(0);
    }

    /**
     * fields_get: Get model field definitions.
     */
    public JsonNode fieldsGet(String baseUrl, String database, String apiKey, String model) {
        return call(baseUrl, database, apiKey, model, "fields_get", Map.of());
    }

    /**
     * Test connection by calling res.users/search_read with limit 1.
     */
    public JsonNode testConnection(String baseUrl, String database, String apiKey) {
        return searchRead(baseUrl, database, apiKey, "res.users",
                List.of(), List.of("name", "login"), 1);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
