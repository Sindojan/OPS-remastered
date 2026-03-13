package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class AnalyzeCodebaseTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeCodebaseTool.class);
    private static final Set<String> ALLOWED_METRICS = Set.of("lines_of_code", "file_count", "package_structure");
    private static final int TIMEOUT_SECONDS = 10;

    private final ObjectMapper objectMapper;

    public AnalyzeCodebaseTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "analyze_codebase";
    }

    @Override
    public String getDescription() {
        return "Analysiert die Codebasis nach Metriken: lines_of_code, file_count oder package_structure.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "metric":{"type":"string","enum":["lines_of_code","file_count","package_structure"],
                        "description":"Die zu analysierende Metrik"}
            },"required":["metric"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String metric = node.has("metric") ? node.get("metric").asText() : null;

            if (metric == null || !ALLOWED_METRICS.contains(metric)) {
                return SystemToolResult.error("Ungültige Metrik. Erlaubt: " + ALLOWED_METRICS);
            }

            String projectRoot = System.getProperty("user.dir");
            Path srcPath = Path.of(projectRoot, "src", "main", "java");

            String result = switch (metric) {
                case "lines_of_code" -> executeLinesOfCode(srcPath);
                case "file_count" -> executeFileCount(srcPath);
                case "package_structure" -> executePackageStructure(srcPath);
                default -> throw new IllegalStateException("Unbekannte Metrik: " + metric);
            };

            return SystemToolResult.success(result);
        } catch (Exception e) {
            log.error("Error analyzing codebase: {}", e.getMessage());
            return SystemToolResult.error("Fehler bei der Codebasis-Analyse: " + e.getMessage());
        }
    }

    private String executeLinesOfCode(Path srcPath) throws IOException {
        long totalLines = 0;
        try (Stream<Path> files = Files.walk(srcPath)) {
            var javaFiles = files.filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p)).toList();
            for (Path file : javaFiles) {
                totalLines += Files.lines(file).count();
            }
        }
        return "## Lines of Code (Java)\n\nGesamt: **" + totalLines + "** Zeilen";
    }

    private String executeFileCount(Path srcPath) throws IOException {
        long count;
        try (Stream<Path> files = Files.walk(srcPath)) {
            count = files.filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p)).count();
        }
        return "## Java File Count\n\nGesamt: **" + count + "** Dateien";
    }

    private String executePackageStructure(Path srcPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> dirs = Files.walk(srcPath)) {
            dirs.filter(Files::isDirectory)
                    .sorted()
                    .limit(40)
                    .forEach(d -> sb.append(d.toAbsolutePath()).append("\n"));
        }
        return "## Package Structure\n\n```\n" + sb.toString().trim() + "\n```";
    }
}
