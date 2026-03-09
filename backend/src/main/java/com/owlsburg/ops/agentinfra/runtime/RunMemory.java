package com.owlsburg.ops.agentinfra.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory scratchpad that lives only during a single agent run.
 * Thread-safe for parallel tool execution.
 */
public final class RunMemory {

    private final ConcurrentHashMap<String, String> entries = new ConcurrentHashMap<>();

    public void put(String key, String value) {
        entries.put(key, value);
    }

    public String get(String key) {
        return entries.get(key);
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }

    public String buildSummary() {
        if (entries.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("## Arbeitsnotizen\n");
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
