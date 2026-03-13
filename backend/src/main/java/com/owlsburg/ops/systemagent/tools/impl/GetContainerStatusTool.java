package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Component
public class GetContainerStatusTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetContainerStatusTool.class);
    private static final int TIMEOUT_SECONDS = 10;

    private final ObjectMapper objectMapper;

    public GetContainerStatusTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_container_status";
    }

    @Override
    public String getDescription() {
        return "Zeigt den Status aller Docker-Container (Name, Status, Ports).";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps",
                    "--format", "{{.Names}}\t{{.Status}}\t{{.Ports}}");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return SystemToolResult.error("Docker-Befehl Timeout nach " + TIMEOUT_SECONDS + "s.");
            }

            if (process.exitValue() != 0) {
                return SystemToolResult.error("Docker nicht erreichbar.");
            }

            String rawOutput = output.toString().trim();
            if (rawOutput.isEmpty()) {
                return SystemToolResult.success("Keine laufenden Container gefunden.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("## Docker Container Status\n\n");
            sb.append("| Name | Status | Ports |\n");
            sb.append("|------|--------|-------|\n");

            for (String line : rawOutput.split("\n")) {
                String[] parts = line.split("\t", -1);
                String name = parts.length > 0 ? parts[0] : "–";
                String status = parts.length > 1 ? parts[1] : "–";
                String ports = parts.length > 2 ? parts[2] : "–";
                sb.append("| ").append(name)
                        .append(" | ").append(status)
                        .append(" | ").append(ports.isEmpty() ? "–" : ports)
                        .append(" |\n");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error getting container status: {}", e.getMessage());
            return SystemToolResult.error("Docker nicht erreichbar.");
        }
    }
}
