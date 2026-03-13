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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
public class ScanSecurityLogsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(ScanSecurityLogsTool.class);
    private static final int TIMEOUT_SECONDS = 15;

    private static final Map<String, Pattern> SECURITY_PATTERNS = new LinkedHashMap<>();
    static {
        SECURITY_PATTERNS.put("401 Unauthorized", Pattern.compile("\\b401\\b", Pattern.CASE_INSENSITIVE));
        SECURITY_PATTERNS.put("403 Forbidden", Pattern.compile("\\b403\\b", Pattern.CASE_INSENSITIVE));
        SECURITY_PATTERNS.put("Login fehlgeschlagen", Pattern.compile("login.*fail|authentication.*fail", Pattern.CASE_INSENSITIVE));
        SECURITY_PATTERNS.put("Rate Limit", Pattern.compile("rate.?limit", Pattern.CASE_INSENSITIVE));
        SECURITY_PATTERNS.put("Unauthorized", Pattern.compile("\\bunauthorized\\b", Pattern.CASE_INSENSITIVE));
        SECURITY_PATTERNS.put("Forbidden", Pattern.compile("\\bforbidden\\b", Pattern.CASE_INSENSITIVE));
    }

    private final ObjectMapper objectMapper;

    public ScanSecurityLogsTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "scan_security_logs";
    }

    @Override
    public String getDescription() {
        return "Scannt Backend-Logs nach sicherheitsrelevanten Einträgen (401, 403, Login-Fehler, Rate-Limits).";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "hours":{"type":"integer","minimum":1,"maximum":168,"description":"Zeitraum in Stunden (default: 24)"}
            },"required":[]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            int hours = node.has("hours") ? node.get("hours").asInt() : 24;
            if (hours < 1) hours = 1;
            if (hours > 168) hours = 168;

            // Parameterized command – no shell invocation
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "logs", "owlsburg-backend", "--since", hours + "h");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Filter in Java using the same security patterns
            Pattern securityFilter = Pattern.compile("(401|403|login.*fail|rate.?limit|unauthorized|forbidden)", Pattern.CASE_INSENSITIVE);
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 500) {
                    if (securityFilter.matcher(line).find()) {
                        output.append(line).append("\n");
                        lineCount++;
                    }
                }
            }

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return SystemToolResult.error("Log-Scan Timeout nach " + TIMEOUT_SECONDS + "s.");
            }

            String logOutput = output.toString();

            // Count occurrences per category
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (Map.Entry<String, Pattern> entry : SECURITY_PATTERNS.entrySet()) {
                int count = 0;
                for (String line : logOutput.split("\n")) {
                    if (entry.getValue().matcher(line).find()) {
                        count++;
                    }
                }
                counts.put(entry.getKey(), count);
            }

            int totalFindings = counts.values().stream().mapToInt(Integer::intValue).sum();

            StringBuilder sb = new StringBuilder();
            sb.append("## Security Log Scan (letzte ").append(hours).append("h)\n\n");

            if (totalFindings == 0) {
                sb.append("Keine sicherheitsrelevanten Einträge gefunden.\n");
            } else {
                sb.append("**Gesamt:** ").append(totalFindings).append(" Einträge\n\n");
                sb.append("| Kategorie | Anzahl |\n");
                sb.append("|-----------|--------|\n");
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    if (entry.getValue() > 0) {
                        sb.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
                    }
                }

                // Show last 10 relevant lines as sample
                String[] lines = logOutput.split("\n");
                int sampleStart = Math.max(0, lines.length - 10);
                sb.append("\n### Letzte Einträge (Beispiel)\n\n```\n");
                for (int i = sampleStart; i < lines.length && !lines[i].isBlank(); i++) {
                    sb.append(lines[i]).append("\n");
                }
                sb.append("```");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error scanning security logs: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Security-Log-Scan: " + e.getMessage());
        }
    }
}
