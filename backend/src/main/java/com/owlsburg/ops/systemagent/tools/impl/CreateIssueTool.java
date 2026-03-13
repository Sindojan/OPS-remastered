package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.systemagent.SystemApiCredentialService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class CreateIssueTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(CreateIssueTool.class);
    private static final String DEFAULT_REPO = "Sindojan/OPS-remastered";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final SystemApiCredentialService credentialService;
    private final ObjectMapper objectMapper;

    public CreateIssueTool(SystemApiCredentialService credentialService, ObjectMapper objectMapper) {
        this.credentialService = credentialService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "create_issue";
    }

    @Override
    public String getDescription() {
        return "Erstellt ein neues GitHub Issue im Repository.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "title":{"type":"string","description":"Titel des Issues"},
              "body":{"type":"string","description":"Beschreibung des Issues"},
              "labels":{"type":"array","items":{"type":"string"},"description":"Labels (optional)"},
              "repo":{"type":"string","description":"GitHub Repository (owner/name), default: Sindojan/OPS-remastered"}
            },"required":["title","body"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);

            String title = node.has("title") ? node.get("title").asText() : null;
            String body = node.has("body") ? node.get("body").asText() : null;
            String repo = node.has("repo") ? node.get("repo").asText() : DEFAULT_REPO;

            if (title == null || title.isBlank()) {
                return SystemToolResult.error("Titel ist erforderlich.");
            }
            if (body == null || body.isBlank()) {
                return SystemToolResult.error("Beschreibung ist erforderlich.");
            }

            String token = credentialService.getDecryptedCredential("github_token")
                    .orElse(null);
            if (token == null) {
                return SystemToolResult.error("Kein GitHub-Token konfiguriert. Bitte 'github_token' in den System-API-Credentials hinterlegen.");
            }

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("title", title);
            requestBody.put("body", body);

            if (node.has("labels") && node.get("labels").isArray()) {
                ArrayNode labels = requestBody.putArray("labels");
                for (JsonNode label : node.get("labels")) {
                    labels.add(label.asText());
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + repo + "/issues"))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                JsonNode responseBody = objectMapper.readTree(response.body());
                String issueUrl = responseBody.has("html_url") ? responseBody.get("html_url").asText() : "–";
                int issueNumber = responseBody.has("number") ? responseBody.get("number").asInt() : 0;
                return SystemToolResult.success("Issue #" + issueNumber + " erstellt: " + issueUrl);
            } else {
                return SystemToolResult.error("GitHub API Fehler: HTTP " + response.statusCode() + " – " + response.body());
            }
        } catch (Exception e) {
            log.error("Error creating issue: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Erstellen des Issues: " + e.getMessage());
        }
    }
}
