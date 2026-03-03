package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentInstanceRepository;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryEntity;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryService;
import com.owlsburg.ops.agentinfra.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ReadAgentMemoryTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ReadAgentMemoryTool.class);

    private final AgentMemoryService memoryService;
    private final AgentInstanceRepository instanceRepository;
    private final ObjectMapper objectMapper;

    public ReadAgentMemoryTool(AgentMemoryService memoryService,
                                AgentInstanceRepository instanceRepository,
                                ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.instanceRepository = instanceRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "read_agent_memory";
    }

    @Override
    public String getDescription() {
        return "Liest die Erinnerungen eines anderen Agents (nur für CEO). " +
                "Ermöglicht Einblick in das Wissen der Lead-Agents.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "agent_name":{"type":"string","description":"Name des Agents (z.B. Production Lead, Machine Lead)"}
            },"required":["agent_name"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String agentName = node.get("agent_name").asText();

            // Find instance by name within tenant (RLS ensures tenant isolation)
            UUID tenantId = UUID.fromString(context.tenantId());
            List<AgentInstanceEntity> instances = instanceRepository.findByTenantId(tenantId);
            AgentInstanceEntity target = instances.stream()
                    .filter(i -> i.getName().equalsIgnoreCase(agentName))
                    .findFirst()
                    .orElse(null);

            if (target == null) {
                return ToolResult.error("Agent nicht gefunden: " + agentName);
            }

            List<AgentMemoryEntity> memories = memoryService.readAgentMemories(target.getId());

            if (memories.isEmpty()) {
                return ToolResult.success("{\"agent\":\"" + agentName + "\",\"memories\":[],\"message\":\"Keine Erinnerungen vorhanden\"}");
            }

            StringBuilder sb = new StringBuilder("{\"agent\":\"").append(agentName).append("\",\"memories\":[");
            for (int i = 0; i < memories.size(); i++) {
                AgentMemoryEntity m = memories.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"type\":\"").append(m.getType())
                  .append("\",\"category\":\"").append(m.getCategory())
                  .append("\",\"key\":\"").append(m.getKey())
                  .append("\",\"value\":").append(objectMapper.writeValueAsString(m.getValue()))
                  .append(",\"importance\":").append(m.getImportance())
                  .append("}");
            }
            sb.append("]}");

            return ToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error reading agent memory: {}", e.getMessage());
            return ToolResult.error("Fehler beim Lesen: " + e.getMessage());
        }
    }
}
