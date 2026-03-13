package com.owlsburg.ops.systemagent.runtime;

import com.owlsburg.ops.agentinfra.runtime.AgentActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Global event bus for system agents (no tenant key needed).
 */
@Component
public class SystemAgentActivityBus {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentActivityBus.class);
    private static final String GLOBAL_KEY = "SYSTEM";

    private final ConcurrentHashMap<String, Set<Consumer<AgentActivityEvent>>> listeners =
            new ConcurrentHashMap<>();

    public Registration subscribe(Consumer<AgentActivityEvent> listener) {
        listeners.computeIfAbsent(GLOBAL_KEY, k -> ConcurrentHashMap.newKeySet()).add(listener);
        return () -> {
            listeners.computeIfPresent(GLOBAL_KEY, (key, set) -> {
                set.remove(listener);
                return set.isEmpty() ? null : set;
            });
        };
    }

    public void publish(AgentActivityEvent event) {
        Set<Consumer<AgentActivityEvent>> set = listeners.get(GLOBAL_KEY);
        if (set == null || set.isEmpty()) return;
        for (Consumer<AgentActivityEvent> listener : set) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.debug("System activity listener error: {}", e.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface Registration {
        void unsubscribe();
    }
}
