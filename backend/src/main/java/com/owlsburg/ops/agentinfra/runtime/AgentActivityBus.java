package com.owlsburg.ops.agentinfra.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class AgentActivityBus {

    private static final Logger log = LoggerFactory.getLogger(AgentActivityBus.class);

    private final ConcurrentHashMap<String, Set<Consumer<AgentActivityEvent>>> listeners =
            new ConcurrentHashMap<>();

    public Registration subscribe(String tenantId, Consumer<AgentActivityEvent> listener) {
        listeners.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet()).add(listener);
        return () -> {
            listeners.computeIfPresent(tenantId, (key, set) -> {
                set.remove(listener);
                return set.isEmpty() ? null : set;
            });
        };
    }

    public void publish(String tenantId, AgentActivityEvent event) {
        Set<Consumer<AgentActivityEvent>> set = listeners.get(tenantId);
        if (set == null || set.isEmpty()) return;
        for (Consumer<AgentActivityEvent> listener : set) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.debug("Activity listener error: {}", e.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface Registration {
        void unsubscribe();
    }
}
