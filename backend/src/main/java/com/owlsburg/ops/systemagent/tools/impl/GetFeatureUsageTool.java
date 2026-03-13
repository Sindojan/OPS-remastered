package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.common.ModuleService;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import com.owlsburg.ops.tenant.TenantEntity;
import com.owlsburg.ops.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetFeatureUsageTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetFeatureUsageTool.class);

    private final TenantRepository tenantRepository;
    private final ModuleService moduleService;
    private final ObjectMapper objectMapper;

    public GetFeatureUsageTool(TenantRepository tenantRepository,
                                ModuleService moduleService,
                                ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.moduleService = moduleService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_feature_usage";
    }

    @Override
    public String getDescription() {
        return "Zeigt die Modul-Nutzung über alle Mandanten hinweg. Optional nach Modulname filterbar.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "feature_name":{"type":"string","description":"Modulname zum Filtern (optional, z.B. 'production', 'inventory')"}
            },"required":[]}""";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String featureName = node.has("feature_name") && !node.get("feature_name").isNull()
                    ? node.get("feature_name").asText() : null;

            List<TenantEntity> tenants = tenantRepository.findByActiveTrue();

            if (tenants.isEmpty()) {
                return SystemToolResult.success("Keine aktiven Mandanten vorhanden.");
            }

            Map<String, Integer> moduleUsageCount = new TreeMap<>();
            Map<String, List<String>> moduleToTenants = new TreeMap<>();

            for (TenantEntity tenant : tenants) {
                Set<String> enabledModules = moduleService.getEnabledModules(tenant.getId());
                for (String module : enabledModules) {
                    moduleUsageCount.merge(module, 1, Integer::sum);
                    moduleToTenants.computeIfAbsent(module, k -> new ArrayList<>()).add(tenant.getName());
                }
            }

            if (featureName != null && !featureName.isBlank()) {
                String key = featureName.toLowerCase();
                if (!moduleUsageCount.containsKey(key)) {
                    return SystemToolResult.success("Modul '" + featureName + "' wird von keinem Mandanten genutzt oder existiert nicht.");
                }

                StringBuilder sb = new StringBuilder();
                sb.append("# Modul-Nutzung: ").append(featureName).append("\n\n");
                sb.append("**Aktive Mandanten:** ").append(moduleUsageCount.get(key))
                        .append(" von ").append(tenants.size()).append("\n\n");
                sb.append("**Mandanten:**\n");
                for (String tenantName : moduleToTenants.get(key)) {
                    sb.append("- ").append(tenantName).append("\n");
                }
                return SystemToolResult.success(sb.toString());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("# Feature-Nutzung über alle Mandanten\n\n");
            sb.append("**Aktive Mandanten:** ").append(tenants.size()).append("\n\n");
            sb.append("| Modul | Mandanten | Nutzungsrate |\n");
            sb.append("|-------|-----------|-------------|\n");

            for (Map.Entry<String, Integer> entry : moduleUsageCount.entrySet()) {
                int count = entry.getValue();
                double rate = (double) count / tenants.size() * 100;
                sb.append("| ").append(entry.getKey());
                sb.append(" | ").append(count);
                sb.append(" | ").append(String.format("%.0f%%", rate));
                sb.append(" |\n");
            }

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Feature-Nutzung: {}", e.getMessage());
            return SystemToolResult.error("Fehler: " + e.getMessage());
        }
    }
}
