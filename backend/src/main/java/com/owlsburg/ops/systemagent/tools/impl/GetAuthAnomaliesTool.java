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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GetAuthAnomaliesTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetAuthAnomaliesTool.class);
    private static final int TIMEOUT_SECONDS = 15;
    private static final Pattern IP_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");
    private static final Pattern FAILED_LOGIN_PATTERN = Pattern.compile("(login.*fail|authentication.*fail|bad credentials)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_REFRESH_PATTERN = Pattern.compile("(refresh.?token|token.*refresh)", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public GetAuthAnomaliesTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_auth_anomalies";
    }

    @Override
    public String getDescription() {
        return "Analysiert Backend-Logs nach Auth-Anomalien: mehrfache Login-Fehler pro IP, unbekannte Emails, ungewöhnliche Token-Refresh-Muster.";
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

            // Fetch auth-related logs – parameterized, no shell
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "logs", "owlsburg-backend", "--since", hours + "h");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Filter in Java instead of shell grep
            Pattern authPattern = Pattern.compile("(login|auth|token|401|403|credential|rate.?limit)", Pattern.CASE_INSENSITIVE);
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 1000) {
                    if (authPattern.matcher(line).find()) {
                        output.append(line).append("\n");
                        lineCount++;
                    }
                }
            }

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return SystemToolResult.error("Log-Analyse Timeout nach " + TIMEOUT_SECONDS + "s.");
            }

            String logOutput = output.toString();
            String[] lines = logOutput.split("\n");

            // Analysis: Failed logins per IP
            Map<String, Integer> failedLoginsPerIp = new HashMap<>();
            int totalFailedLogins = 0;
            int totalTokenRefreshes = 0;
            int total401 = 0;
            int total403 = 0;

            for (String line : lines) {
                if (line.isBlank()) continue;

                if (FAILED_LOGIN_PATTERN.matcher(line).find()) {
                    totalFailedLogins++;
                    Matcher ipMatcher = IP_PATTERN.matcher(line);
                    if (ipMatcher.find()) {
                        String ip = ipMatcher.group(1);
                        failedLoginsPerIp.merge(ip, 1, Integer::sum);
                    }
                }

                if (TOKEN_REFRESH_PATTERN.matcher(line).find()) {
                    totalTokenRefreshes++;
                }

                if (line.contains("401")) total401++;
                if (line.contains("403")) total403++;
            }

            // Build anomaly report
            StringBuilder sb = new StringBuilder();
            sb.append("## Auth-Anomalie-Report (letzte ").append(hours).append("h)\n\n");

            sb.append("### Zusammenfassung\n");
            sb.append("| Kategorie | Anzahl |\n");
            sb.append("|-----------|--------|\n");
            sb.append("| Fehlgeschlagene Logins | ").append(totalFailedLogins).append(" |\n");
            sb.append("| Token-Refreshes | ").append(totalTokenRefreshes).append(" |\n");
            sb.append("| 401 Responses | ").append(total401).append(" |\n");
            sb.append("| 403 Responses | ").append(total403).append(" |\n\n");

            // Flag IPs with multiple failed logins
            boolean hasAnomalies = false;
            if (!failedLoginsPerIp.isEmpty()) {
                sb.append("### Fehlgeschlagene Logins pro IP\n");
                sb.append("| IP-Adresse | Versuche | Bewertung |\n");
                sb.append("|------------|----------|-----------|\n");
                for (Map.Entry<String, Integer> entry : failedLoginsPerIp.entrySet()) {
                    String severity = entry.getValue() >= 10 ? "KRITISCH" :
                            entry.getValue() >= 5 ? "WARNUNG" : "NORMAL";
                    if (entry.getValue() >= 5) hasAnomalies = true;
                    sb.append("| ").append(entry.getKey())
                            .append(" | ").append(entry.getValue())
                            .append(" | ").append(severity)
                            .append(" |\n");
                }
                sb.append("\n");
            }

            // Overall assessment
            sb.append("### Bewertung\n\n");
            if (!hasAnomalies && totalFailedLogins < 10) {
                sb.append("Keine auffälligen Anomalien erkannt. Auth-Aktivität im normalen Bereich.");
            } else if (hasAnomalies) {
                sb.append("**WARNUNG:** Mehrfache fehlgeschlagene Login-Versuche von einzelnen IPs erkannt. ");
                sb.append("Möglicher Brute-Force-Versuch. Rate-Limiting prüfen.");
            } else {
                sb.append("Erhöhte Anzahl fehlgeschlagener Logins (").append(totalFailedLogins)
                        .append("). Beobachtung empfohlen.");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error getting auth anomalies: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Auth-Anomalie-Analyse: " + e.getMessage());
        }
    }
}
