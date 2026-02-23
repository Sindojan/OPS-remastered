package com.sindoflow.ops.agentinfra.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderRegistry.class);

    private final Map<String, LlmProvider> providers;

    public LlmProviderRegistry(List<LlmProvider> providerList) {
        this.providers = new HashMap<>();
        for (LlmProvider provider : providerList) {
            providers.put(provider.getProviderName(), provider);
            log.info("Registered LLM provider: {}", provider.getProviderName());
        }
    }

    public LlmProvider getProvider(String name) {
        LlmProvider provider = providers.get(name);
        if (provider == null) {
            throw new LlmProviderException("Unknown LLM provider: " + name
                    + ". Available: " + getAvailableProviders());
        }
        return provider;
    }

    public List<String> getAvailableProviders() {
        return List.copyOf(providers.keySet());
    }
}
