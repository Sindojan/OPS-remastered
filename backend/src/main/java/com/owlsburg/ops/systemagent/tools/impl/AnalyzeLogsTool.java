package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
public class AnalyzeLogsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeLogsTool.class);
    private static final int TIMEOUT_SECONDS = 15;
    private static final int MAX_OUTPUT_BYTES = 10 * 1024;
    private static final Pattern SAFE_FILTER_PATTERN = Pattern.compile("^[a-zA-Z0-9 .\\-_]+$");
    private static final Set<String> ALLOWED_SERVICES = Set.of("backend", "postgres", "minio");
    private static final Map<String, String> CONTAINER_NAMES = Map.of(
            "backend", "owlsburg-backend",
            "postgres", "owlsburg-postgres",
            "minio", "owlsburg-minio"
    );

    private final ObjectMapper objectMapper;

    public AnalyzeLogsTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "analyze_logs";
    }

    @Override
    public String getDescription() {
        return "Analysiert Docker-Container-Logs für backend, postgres oder minio. Optional mit Zeilenanzahl und Grep-Filter.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "service":{"type":"string","enum":["backend","postgres","minio"],"description":"Service-Name"},
              "lines":{"type":"integer","minimum":1,"maximum":1000,"description":"Anzahl Zeilen (default: 100)"},
              "filter":{"type":"string","description":"Grep-Filter (optional, nur alphanumerisch + Leerzeichen/Punkte/Bindestriche)"}
            },"required":["service"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);

            String service = node.has("service") ? node.get("service").asText() : null;
            int lines = node.has("lines") ? node.get("lines").asInt() : 100;
            String filter = node.has("filter") ? node.get("filter").asText() : null;

            if (service == null || !ALLOWED_SERVICES.contains(service)) {
                return SystemToolResult.error("Ungültiger Service. Erlaubt: " + ALLOWED_SERVICES);
            }

            if (lines < 1) lines = 1;
            if (lines > 1000) lines = 1000;

            if (filter != null && !filter.isBlank()) {
                if (!SAFE_FILTER_PATTERN.matcher(filter).matches()) {
                    return SystemToolResult.error("Ungültiger Filter. Nur alphanumerische Zeichen, Leerzeichen, Punkte und Bindestriche erlaubt.");
                }
            }

            String containerName = CONTAINER_NAMES.get(service);

            // Use parameterized ProcessBuilder – no shell invocation
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "logs", containerName, "--tail", String.valueOf(lines));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            String filterLower = (filter != null && !filter.isBlank()) ? filter.toLowerCase() : null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String logLine;
                while ((logLine = reader.readLine()) != null) {
                    // Apply filter in Java instead of shell grep
                    if (filterLower != null && !logLine.toLowerCase().contains(filterLower)) {
                        continue;
                    }
                    if (output.length() + logLine.length() > MAX_OUTPUT_BYTES) {
                        output.append("\n... (abgeschnitten bei 10KB)");
                        break;
                    }
                    output.append(logLine).append("\n");
                }
            }

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return SystemToolResult.error("Log-Abfrage Timeout nach " + TIMEOUT_SECONDS + "s.");
            }

            String logOutput = output.toString().trim();
            if (logOutput.isEmpty()) {
                return SystemToolResult.success("Keine Log-Einträge gefunden für " + service +
                        (filter != null ? " mit Filter '" + filter + "'" : "") + ".");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("## Logs – ").append(service).append(" (").append(containerName).append(")\n");
            if (filter != null && !filter.isBlank()) {
                sb.append("**Filter:** ").append(filter).append("\n");
            }
            sb.append("\n```\n").append(logOutput).append("\n```");

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error analyzing logs: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen der Logs: " + e.getMessage());
        }
    }

}
