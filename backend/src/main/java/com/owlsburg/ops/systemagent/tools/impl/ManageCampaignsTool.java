package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryEntity;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ManageCampaignsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(ManageCampaignsTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public ManageCampaignsTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "manage_campaigns";
    }

    @Override
    public String getDescription() {
        return "Verwaltet Marketing-Kampagnen (Erstellen, Auflisten, Aktualisieren, Löschen). Daten werden im Agent-Memory gespeichert.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "action":{"type":"string","enum":["list","create","update","delete"],"description":"Auszuführende Aktion"},
              "data":{"type":"object","properties":{
                "name":{"type":"string","description":"Kampagnenname"},
                "description":{"type":"string","description":"Beschreibung der Kampagne"},
                "status":{"type":"string","enum":["ENTWURF","AKTIV","PAUSIERT","BEENDET"],"description":"Kampagnenstatus"},
                "id":{"type":"string","description":"Kampagnen-ID (für update/delete)"}
              },"description":"Kampagnendaten (je nach Aktion)"}
            },"required":["action"]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String action = node.get("action").asText();
            JsonNode data = node.has("data") ? node.get("data") : null;

            return switch (action) {
                case "list" -> listCampaigns(context);
                case "create" -> createCampaign(context, data);
                case "update" -> updateCampaign(context, data);
                case "delete" -> deleteCampaign(context, data);
                default -> SystemToolResult.error("Ungültige Aktion: " + action + ". Erlaubt: list, create, update, delete.");
            };
        } catch (Exception e) {
            log.error("Fehler bei der Kampagnenverwaltung: {}", e.getMessage());
            return SystemToolResult.error("Fehler: " + e.getMessage());
        }
    }

    private SystemToolResult listCampaigns(SystemToolExecutionContext context) {
        List<SystemAgentMemoryEntity> campaigns = memoryService.recallMemories(
                context.instanceId(), "campaigns", 50);

        if (campaigns.isEmpty()) {
            return SystemToolResult.success("Keine Kampagnen vorhanden.");
        }

        StringBuilder sb = new StringBuilder("Kampagnen:\n\n");
        for (SystemAgentMemoryEntity m : campaigns) {
            sb.append("- **").append(m.getKey().replace("campaign:", "")).append("**\n");
            sb.append("  ").append(m.getValue()).append("\n\n");
        }

        return SystemToolResult.success(sb.toString());
    }

    private SystemToolResult createCampaign(SystemToolExecutionContext context, JsonNode data) {
        if (data == null || !data.has("name")) {
            return SystemToolResult.error("Kampagnenname ist erforderlich (data.name).");
        }

        String name = data.get("name").asText();
        String description = data.has("description") ? data.get("description").asText() : "";
        String status = data.has("status") ? data.get("status").asText() : "ENTWURF";

        String key = "campaign:" + name;
        String value = "Beschreibung: " + description + "\n" +
                "Status: " + status + "\n" +
                "Erstellt: " + Instant.now();

        memoryService.saveMemory(context.instanceId(), "SEMANTIC", "campaigns", key, value, 6);

        return SystemToolResult.success("Kampagne '" + name + "' erstellt mit Status " + status + ".");
    }

    private SystemToolResult updateCampaign(SystemToolExecutionContext context, JsonNode data) {
        if (data == null || !data.has("name")) {
            return SystemToolResult.error("Kampagnenname ist erforderlich (data.name).");
        }

        String name = data.get("name").asText();
        String key = "campaign:" + name;

        List<SystemAgentMemoryEntity> existing = memoryService.recallMemories(
                context.instanceId(), "campaigns", 50);

        boolean found = existing.stream().anyMatch(m -> m.getKey().equals(key));
        if (!found) {
            return SystemToolResult.error("Kampagne '" + name + "' nicht gefunden.");
        }

        StringBuilder value = new StringBuilder();
        if (data.has("description")) {
            value.append("Beschreibung: ").append(data.get("description").asText()).append("\n");
        }
        if (data.has("status")) {
            value.append("Status: ").append(data.get("status").asText()).append("\n");
        }
        value.append("Aktualisiert: ").append(Instant.now());

        memoryService.saveMemory(context.instanceId(), "SEMANTIC", "campaigns", key, value.toString(), 6);

        return SystemToolResult.success("Kampagne '" + name + "' aktualisiert.");
    }

    private SystemToolResult deleteCampaign(SystemToolExecutionContext context, JsonNode data) {
        if (data == null || (!data.has("name") && !data.has("id"))) {
            return SystemToolResult.error("Kampagnenname (data.name) oder ID (data.id) ist erforderlich.");
        }

        String name = data.has("name") ? data.get("name").asText() : data.get("id").asText();

        // Mark as deleted by updating the value (memory service uses upsert)
        String key = "campaign:" + name;
        memoryService.saveMemory(context.instanceId(), "SEMANTIC", "campaigns", key,
                "Status: GELÖSCHT\nGelöscht: " + Instant.now(), 1);

        return SystemToolResult.success("Kampagne '" + name + "' als gelöscht markiert.");
    }
}
