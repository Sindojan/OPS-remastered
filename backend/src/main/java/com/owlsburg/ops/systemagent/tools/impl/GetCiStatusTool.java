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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GetCiStatusTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetCiStatusTool.class);
    private static final String DEFAULT_REPO = "Sindojan/OPS-remastered";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final SystemApiCredentialService credentialService;
    private final ObjectMapper objectMapper;

    public GetCiStatusTool(SystemApiCredentialService credentialService, ObjectMapper objectMapper) {
        this.credentialService = credentialService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_ci_status";
    }

    @Override
    public String getDescription() {
        return "Ruft den CI/CD-Status der letzten 5 Workflow-Runs von GitHub Actions ab.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "repo":{"type":"string","description":"GitHub Repository (owner/name), default: Sindojan/OPS-remastered"}
            },"required":[]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String repo = node.has("repo") ? node.get("repo").asText() : DEFAULT_REPO;

            String token = credentialService.getDecryptedCredential("github_token")
                    .orElse(null);
            if (token == null) {
                return SystemToolResult.error("Kein GitHub-Token konfiguriert. Bitte 'github_token' in den System-API-Credentials hinterlegen.");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + repo + "/actions/runs?per_page=5"))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return SystemToolResult.error("GitHub API Fehler: HTTP " + response.statusCode());
            }

            JsonNode body = objectMapper.readTree(response.body());
            JsonNode runs = body.get("workflow_runs");

            if (runs == null || !runs.isArray() || runs.isEmpty()) {
                return SystemToolResult.success("Keine Workflow-Runs gefunden für " + repo + ".");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("## CI/CD Status – ").append(repo).append("\n\n");
            sb.append("| Workflow | Status | Ergebnis | Erstellt |\n");
            sb.append("|----------|--------|----------|----------|\n");

            for (JsonNode run : runs) {
                String name = run.has("name") ? run.get("name").asText() : "–";
                String status = run.has("status") ? run.get("status").asText() : "–";
                String conclusion = run.has("conclusion") && !run.get("conclusion").isNull()
                        ? run.get("conclusion").asText() : "–";
                String createdAt = run.has("created_at") ? run.get("created_at").asText() : "–";

                sb.append("| ").append(name)
                        .append(" | ").append(status)
                        .append(" | ").append(conclusion)
                        .append(" | ").append(createdAt)
                        .append(" |\n");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error getting CI status: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen des CI-Status: " + e.getMessage());
        }
    }
}
