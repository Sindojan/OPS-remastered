package com.owlsburg.ops.systemagent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry for all system agent tools. No module filtering (system-level access).
 */
@Component
public class SystemAgentToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentToolRegistry.class);

    private final Map<String, SystemAgentTool> toolMap;

    public SystemAgentToolRegistry(List<SystemAgentTool> tools) {
        this.toolMap = new LinkedHashMap<>();
        for (SystemAgentTool tool : tools) {
            toolMap.put(tool.getName(), tool);
        }
        log.info("Registered {} system agent tools", toolMap.size());
    }

    public Optional<SystemAgentTool> getTool(String name) {
        return Optional.ofNullable(toolMap.get(name));
    }

    public List<SystemAgentTool> getToolsByNames(List<String> names) {
        List<SystemAgentTool> result = new ArrayList<>();
        for (String name : names) {
            SystemAgentTool tool = toolMap.get(name);
            if (tool != null) {
                result.add(tool);
            } else {
                log.warn("System tool not found: {}", name);
            }
        }
        return result;
    }

    public List<String> getAllToolNames() {
        return new ArrayList<>(toolMap.keySet());
    }

    public Collection<SystemAgentTool> getAllTools() {
        return toolMap.values();
    }
}
