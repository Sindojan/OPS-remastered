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
public class GetBuildLogsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetBuildLogsTool.class);
    private static final String DEFAULT_REPO = "Sindojan/OPS-remastered";
    private static final int MAX_LOG_BYTES = 5 * 1024;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final SystemApiCredentialService credentialService;
    private final ObjectMapper objectMapper;

    public GetBuildLogsTool(SystemApiCredentialService credentialService, ObjectMapper objectMapper) {
        this.credentialService = credentialService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_build_logs";
    }

    @Override
    public String getDescription() {
        return "Ruft die Build-Logs eines GitHub Actions Workflow-Runs ab (max. 5KB).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "run_id":{"type":"string","description":"Die ID des Workflow-Runs"},
              "repo":{"type":"string","description":"GitHub Repository (owner/name), default: Sindojan/OPS-remastered"}
            },"required":["run_id"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);

            String runId = node.has("run_id") ? node.get("run_id").asText() : null;
            String repo = node.has("repo") ? node.get("repo").asText() : DEFAULT_REPO;

            if (runId == null || runId.isBlank()) {
                return SystemToolResult.error("run_id ist erforderlich.");
            }

            if (!runId.matches("\\d+")) {
                return SystemToolResult.error("run_id muss eine numerische ID sein.");
            }

            String token = credentialService.getDecryptedCredential("github_token")
                    .orElse(null);
            if (token == null) {
                return SystemToolResult.error("Kein GitHub-Token konfiguriert. Bitte 'github_token' in den System-API-Credentials hinterlegen.");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + repo + "/actions/runs/" + runId + "/logs"))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String logContent = response.body();
                if (logContent.length() > MAX_LOG_BYTES) {
                    logContent = logContent.substring(0, MAX_LOG_BYTES) + "\n\n... (abgeschnitten bei 5KB)";
                }
                return SystemToolResult.success("## Build Logs – Run #" + runId + "\n\n```\n" + logContent + "\n```");
            } else if (response.statusCode() == 404) {
                return SystemToolResult.error("Workflow-Run #" + runId + " nicht gefunden.");
            } else {
                return SystemToolResult.error("GitHub API Fehler: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Error getting build logs: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen der Build-Logs: " + e.getMessage());
        }
    }
}
