package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryEntity;
import com.owlsburg.ops.systemagent.memory.SystemAgentMemoryService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetMarketingKpisTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetMarketingKpisTool.class);

    private final SystemAgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public GetMarketingKpisTool(SystemAgentMemoryService memoryService, ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_marketing_kpis";
    }

    @Override
    public String getDescription() {
        return "Gibt aggregierte Marketing-KPIs zurück: Anzahl Kampagnen, Social-Media-Beiträge und deren Status.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{},"required":[]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            List<SystemAgentMemoryEntity> campaigns = memoryService.recallMemories(
                    context.instanceId(), "campaigns", 50);
            List<SystemAgentMemoryEntity> posts = memoryService.recallMemories(
                    context.instanceId(), "social_posts", 50);

            long activeCampaigns = campaigns.stream()
                    .filter(m -> m.getValue().contains("Status: AKTIV"))
                    .count();
            long draftCampaigns = campaigns.stream()
                    .filter(m -> m.getValue().contains("Status: ENTWURF"))
                    .count();
            long deletedCampaigns = campaigns.stream()
                    .filter(m -> m.getValue().contains("Status: GELÖSCHT"))
                    .count();
            long totalCampaigns = campaigns.size() - deletedCampaigns;

            long twitterPosts = posts.stream()
                    .filter(m -> m.getKey().contains("twitter"))
                    .count();
            long linkedinPosts = posts.stream()
                    .filter(m -> m.getKey().contains("linkedin"))
                    .count();
            long instagramPosts = posts.stream()
                    .filter(m -> m.getKey().contains("instagram"))
                    .count();

            StringBuilder sb = new StringBuilder("# Marketing-KPIs\n\n");

            sb.append("## Kampagnen\n");
            sb.append("| Metrik | Wert |\n");
            sb.append("|--------|------|\n");
            sb.append("| Gesamt (aktiv) | ").append(totalCampaigns).append(" |\n");
            sb.append("| Aktiv | ").append(activeCampaigns).append(" |\n");
            sb.append("| Entwurf | ").append(draftCampaigns).append(" |\n\n");

            sb.append("## Social-Media-Beiträge\n");
            sb.append("| Plattform | Anzahl |\n");
            sb.append("|-----------|--------|\n");
            sb.append("| Twitter | ").append(twitterPosts).append(" |\n");
            sb.append("| LinkedIn | ").append(linkedinPosts).append(" |\n");
            sb.append("| Instagram | ").append(instagramPosts).append(" |\n");
            sb.append("| **Gesamt** | **").append(posts.size()).append("** |\n");

            if (campaigns.isEmpty() && posts.isEmpty()) {
                sb.append("\nHinweis: Noch keine Marketing-Daten vorhanden. Erstelle Kampagnen mit manage_campaigns oder Beiträge mit social_media_post.");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Marketing-KPIs: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen: " + e.getMessage());
        }
    }
}
