package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.SystemApiCredentialService;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class SocialMediaPostTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(SocialMediaPostTool.class);

    private final SystemApiCredentialService credentialService;
    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public SocialMediaPostTool(SystemApiCredentialService credentialService,
                               SystemAgentMemoryService memoryService,
                               ObjectMapper objectMapper) {
        this.credentialService = credentialService;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "social_media_post";
    }

    @Override
    public String getDescription() {
        return "Erstellt einen Social-Media-Beitrag auf der angegebenen Plattform. Im MVP-Modus wird der Beitrag im Memory gespeichert.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "platform":{"type":"string","enum":["twitter","linkedin","instagram"],"description":"Zielplattform"},
              "content":{"type":"string","description":"Beitragsinhalt"},
              "schedule_time":{"type":"string","description":"Geplanter Veröffentlichungszeitpunkt (ISO-8601, optional)"}
            },"required":["platform","content"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String platform = node.get("platform").asText();
            String content = node.get("content").asText();
            String scheduleTime = node.has("schedule_time") ? node.get("schedule_time").asText() : null;

            if (!platform.matches("twitter|linkedin|instagram")) {
                return SystemToolResult.error("Ungültige Plattform: " + platform + ". Erlaubt: twitter, linkedin, instagram.");
            }

            if (content.isBlank()) {
                return SystemToolResult.error("Beitragsinhalt darf nicht leer sein.");
            }

            Optional<String> credential = credentialService.getDecryptedCredential(platform + "_api");
            if (credential.isEmpty()) {
                log.info("Keine API-Credentials für Plattform '{}' konfiguriert – speichere Beitrag im Memory (MVP-Modus)", platform);
            }

            String timestamp = Instant.now().toString();
            String key = "post:" + platform + ":" + timestamp;
            StringBuilder value = new StringBuilder();
            value.append("Plattform: ").append(platform).append("\n");
            value.append("Inhalt: ").append(content).append("\n");
            value.append("Erstellt: ").append(timestamp).append("\n");
            if (scheduleTime != null && !scheduleTime.isBlank()) {
                value.append("Geplant für: ").append(scheduleTime).append("\n");
            }
            value.append("Status: ENTWURF (MVP – keine API-Integration)");

            memoryService.saveMemory(context.instanceId(), "SEMANTIC", "social_posts", key, value.toString(), 5);

            StringBuilder result = new StringBuilder();
            result.append("Social-Media-Beitrag gespeichert (MVP-Modus):\n");
            result.append("- Plattform: ").append(platform).append("\n");
            result.append("- Inhalt: ").append(content.length() > 100 ? content.substring(0, 100) + "..." : content).append("\n");
            if (scheduleTime != null && !scheduleTime.isBlank()) {
                result.append("- Geplant für: ").append(scheduleTime).append("\n");
            }
            result.append("- Status: ENTWURF\n");
            if (credential.isEmpty()) {
                result.append("\nHinweis: API-Credentials für '").append(platform).append("' sind noch nicht konfiguriert. Beitrag wurde nur im Memory gespeichert.");
            }

            return SystemToolResult.success(result.toString());
        } catch (Exception e) {
            log.error("Fehler beim Erstellen des Social-Media-Beitrags: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Erstellen: " + e.getMessage());
        }
    }
}
