package com.owlsburg.ops.agentinfra.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentTemplateEntity;
import com.owlsburg.ops.common.ModuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AgentToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentToolRegistry.class);

    private final Map<String, AgentTool> toolMap;
    private final ObjectMapper objectMapper;
    private final ModuleService moduleService;

    public AgentToolRegistry(List<AgentTool> tools, ObjectMapper objectMapper, ModuleService moduleService) {
        this.objectMapper = objectMapper;
        this.moduleService = moduleService;
        this.toolMap = tools.stream()
                .collect(Collectors.toMap(AgentTool::getName, t -> t));
        log.info("AgentToolRegistry initialized with {} tools: {}", toolMap.size(), toolMap.keySet());
    }

    public List<AgentTool> getToolsForInstance(AgentTemplateEntity template) {
        return getToolsForInstance(template, null);
    }

    public List<AgentTool> getToolsForInstance(AgentTemplateEntity template, UUID tenantId) {
        try {
            List<String> allowedToolNames = objectMapper.readValue(
                    template.getAllowedTools(), new TypeReference<List<String>>() {});

            return allowedToolNames.stream()
                    .map(toolMap::get)
                    .filter(Objects::nonNull)
                    .filter(tool -> tenantId == null || moduleService.isModuleEnabled(tenantId, tool.getModuleId()))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to parse allowedTools JSON for template {}: {}",
                    template.getId(), e.getMessage());
            return List.of();
        }
    }

    public Optional<AgentTool> getTool(String name) {
        return Optional.ofNullable(toolMap.get(name));
    }

    public Collection<AgentTool> getAllTools() {
        return Collections.unmodifiableCollection(toolMap.values());
    }

    public List<AgentTool> getToolsByNames(List<String> names, UUID tenantId) {
        return names.stream()
                .map(toolMap::get)
                .filter(Objects::nonNull)
                .filter(tool -> tenantId == null || moduleService.isModuleEnabled(tenantId, tool.getModuleId()))
                .toList();
    }

    public List<String> getAllToolNames() {
        return List.copyOf(toolMap.keySet());
    }
}
