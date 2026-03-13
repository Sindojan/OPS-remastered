package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.SystemApiCredentialService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
public class WebSearchTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final SystemApiCredentialService credentialService;
    private final ObjectMapper objectMapper;

    public WebSearchTool(SystemApiCredentialService credentialService, ObjectMapper objectMapper) {
        this.credentialService = credentialService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "Durchsucht das Web über SerpAPI. Benötigt konfigurierte API-Credentials für 'search_api'.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "query":{"type":"string","description":"Suchbegriff"},
              "max_results":{"type":"integer","minimum":1,"maximum":20,"description":"Maximale Anzahl Ergebnisse (optional, default: 5)"}
            },"required":["query"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String query = node.get("query").asText();
            int maxResults = node.has("max_results") ? node.get("max_results").asInt() : 5;

            if (query.isBlank()) {
                return SystemToolResult.error("Suchbegriff darf nicht leer sein.");
            }

            Optional<String> apiKey = credentialService.getDecryptedCredential("search_api");
            if (apiKey.isEmpty()) {
                return SystemToolResult.error(
                        "Kein API-Key für 'search_api' konfiguriert. " +
                        "Bitte in der Systemverwaltung unter API-Credentials einen SerpAPI-Key hinterlegen.");
            }

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://serpapi.com/search?q=" + encodedQuery +
                    "&api_key=" + apiKey.get() +
                    "&num=" + maxResults;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("SerpAPI returned status {}: {}", response.statusCode(), response.body());
                return SystemToolResult.error("SerpAPI-Fehler (HTTP " + response.statusCode() + "). Prüfe den API-Key.");
            }

            JsonNode resultJson = objectMapper.readTree(response.body());
            JsonNode organicResults = resultJson.get("organic_results");

            if (organicResults == null || !organicResults.isArray() || organicResults.isEmpty()) {
                return SystemToolResult.success("Keine Ergebnisse für '" + query + "' gefunden.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Suchergebnisse für '").append(query).append("':\n\n");

            int count = 0;
            for (JsonNode result : organicResults) {
                if (count >= maxResults) break;
                count++;

                String title = result.has("title") ? result.get("title").asText() : "Ohne Titel";
                String link = result.has("link") ? result.get("link").asText() : "";
                String snippet = result.has("snippet") ? result.get("snippet").asText() : "";

                sb.append(count).append(". **").append(title).append("**\n");
                sb.append("   URL: ").append(link).append("\n");
                if (!snippet.isBlank()) {
                    sb.append("   ").append(snippet).append("\n");
                }
                sb.append("\n");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Fehler bei der Web-Suche: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Web-Suche: " + e.getMessage());
        }
    }
}
