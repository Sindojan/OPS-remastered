package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentInstanceRepository;
import com.owlsburg.ops.agentinfra.AgentInstanceStatus;
import com.owlsburg.ops.agentinfra.AgentInstanceType;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryService;
import com.owlsburg.ops.agentinfra.runtime.*;
import com.owlsburg.ops.agentinfra.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SpawnSubAgentTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(SpawnSubAgentTool.class);
    private static final int MAX_SUB_AGENTS_PER_LEAD = 3;

    private final AgentFactory agentFactory;
    private final AgentInstanceRepository instanceRepository;
    private final AgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public SpawnSubAgentTool(AgentFactory agentFactory,
                             AgentInstanceRepository instanceRepository,
                             AgentMemoryService memoryService,
                             ObjectMapper objectMapper) {
        this.agentFactory = agentFactory;
        this.instanceRepository = instanceRepository;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "spawn_sub_agent";
    }

    @Override
    public String getDescription() {
        return "Erstellt einen spezialisierten Sub-Agent für eine Teilaufgabe. " +
                "Der Sub-Agent arbeitet mit einem Subset deiner Tools und gibt sein Ergebnis zurück.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "task":{"type":"string","description":"Aufgabenbeschreibung für den Sub-Agent"},
              "tools":{"type":"array","items":{"type":"string"},"description":"Liste der Tool-Namen die der Sub-Agent nutzen darf (muss Subset deiner Tools sein)"},
              "name":{"type":"string","description":"Name des Sub-Agents (z.B. CNC-3 Wartungsanalyse)"}
            },"required":["task","tools","name"]}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.WRITE_WITH_APPROVAL;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode node = objectMapper.readTree(input);
            String task = node.get("task").asText();
            List<String> tools = objectMapper.readValue(
                    node.get("tools").toString(), new TypeReference<List<String>>() {});
            String name = node.get("name").asText();

            // Check max sub-agents limit
            long activeSubAgents = instanceRepository.findByTenantId(
                    UUID.fromString(context.tenantId())).stream()
                    .filter(i -> i.getType() == AgentInstanceType.EPHEMERAL
                            && i.getStatus() == AgentInstanceStatus.ACTIVE
                            && context.instanceId().equals(i.getParentInstanceId()))
                    .count();

            if (activeSubAgents >= MAX_SUB_AGENTS_PER_LEAD) {
                return ToolResult.error("Maximale Anzahl aktiver Sub-Agents erreicht (" + MAX_SUB_AGENTS_PER_LEAD + ")");
            }

            // Create parent Agent object for factory
            Agent parentAgent = agentFactory.createAgent(context.instanceId(), context.tenantId());

            // Spawn sub-agent
            SubAgent subAgent = agentFactory.spawnSubAgent(parentAgent, task, tools, name, context.tenantId());

            log.info("Spawned sub-agent '{}' with tools: {}", name, tools);

            // Execute synchronously
            AgentContext agentContext = new AgentContext(
                    UUID.randomUUID(), context.tenantId(), null, null, 2, Instant.now());
            AgentResult result = subAgent.execute(agentContext, task);

            // Terminate and cleanup
            UUID subInstanceId = subAgent.identity().instanceId();
            terminateSubAgent(subInstanceId, context.instanceId());

            log.info("Sub-agent '{}' completed (status: {}, tokens: {}+{})",
                    name, result.status(), result.inputTokens(), result.outputTokens());

            return ToolResult.success(result.output());
        } catch (IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error spawning sub-agent: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Erstellen des Sub-Agents: " + e.getMessage());
        }
    }

    private void terminateSubAgent(UUID subInstanceId, UUID parentInstanceId) {
        try {
            // Promote important memories to parent
            memoryService.promoteMemories(subInstanceId, parentInstanceId, 7);

            // Delete remaining memories
            memoryService.deleteAllForInstance(subInstanceId);

            // Mark as terminated
            instanceRepository.findById(subInstanceId).ifPresent(instance -> {
                instance.setStatus(AgentInstanceStatus.TERMINATED);
                instance.setTerminatedAt(Instant.now());
                instanceRepository.save(instance);
            });
        } catch (Exception e) {
            log.error("Error terminating sub-agent {}: {}", subInstanceId, e.getMessage());
        }
    }
}
