package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.regex.Pattern;

@Component
public class ScrapeWebpageTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(ScrapeWebpageTool.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final int MAX_RESPONSE_BYTES = 50 * 1024;
    private static final int MAX_OUTPUT_CHARS = 10 * 1024;
    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
            "^(https?://)?(10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|127\\.|0\\.|localhost|\\[::1\\])"
    );
    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile(
            "<(script|style|noscript)[^>]*>.*?</\\1>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s{3,}");

    private final ObjectMapper objectMapper;

    public ScrapeWebpageTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "scrape_webpage";
    }

    @Override
    public String getDescription() {
        return "Ruft eine Webseite ab und extrahiert den Textinhalt. Blockiert private IP-Adressen aus Sicherheitsgründen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "url":{"type":"string","description":"URL der Webseite (muss mit http:// oder https:// beginnen)"},
              "selectors":{"type":"string","description":"CSS-Selektoren zum Filtern (optional, Hinweis: vereinfachte Extraktion)"}
            },"required":["url"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String url = node.get("url").asText();

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return SystemToolResult.error("URL muss mit http:// oder https:// beginnen.");
            }

            if (PRIVATE_IP_PATTERN.matcher(url).find()) {
                return SystemToolResult.error("Zugriff auf private/lokale Adressen ist aus Sicherheitsgründen blockiert.");
            }

            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                return SystemToolResult.error("Ungültige URL: Host konnte nicht ermittelt werden.");
            }

            if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1") ||
                host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("172.16.") ||
                host.startsWith("172.17.") || host.startsWith("172.18.") || host.startsWith("172.19.") ||
                host.startsWith("172.20.") || host.startsWith("172.21.") || host.startsWith("172.22.") ||
                host.startsWith("172.23.") || host.startsWith("172.24.") || host.startsWith("172.25.") ||
                host.startsWith("172.26.") || host.startsWith("172.27.") || host.startsWith("172.28.") ||
                host.startsWith("172.29.") || host.startsWith("172.30.") || host.startsWith("172.31.")) {
                return SystemToolResult.error("Zugriff auf private/lokale Adressen ist aus Sicherheitsgründen blockiert.");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "OwlsburgOPS-Bot/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return SystemToolResult.error("HTTP-Fehler " + response.statusCode() + " beim Abrufen von " + url);
            }

            String body = response.body();
            if (body.length() > MAX_RESPONSE_BYTES) {
                body = body.substring(0, MAX_RESPONSE_BYTES);
            }

            String cleaned = stripHtml(body);

            if (cleaned.length() > MAX_OUTPUT_CHARS) {
                cleaned = cleaned.substring(0, MAX_OUTPUT_CHARS) + "\n\n[... gekürzt auf 10KB ...]";
            }

            if (cleaned.isBlank()) {
                return SystemToolResult.success("Die Seite enthält keinen extrahierbaren Textinhalt.");
            }

            return SystemToolResult.success("Inhalt von " + url + ":\n\n" + cleaned);
        } catch (IllegalArgumentException e) {
            return SystemToolResult.error("Ungültige URL: " + e.getMessage());
        } catch (Exception e) {
            log.error("Fehler beim Scraping von Webseite: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen: " + e.getMessage());
        }
    }

    private String stripHtml(String html) {
        String result = SCRIPT_STYLE_PATTERN.matcher(html).replaceAll(" ");
        result = HTML_TAG_PATTERN.matcher(result).replaceAll(" ");
        result = result.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ");
        result = WHITESPACE_PATTERN.matcher(result).replaceAll("\n\n");
        return result.strip();
    }
}
