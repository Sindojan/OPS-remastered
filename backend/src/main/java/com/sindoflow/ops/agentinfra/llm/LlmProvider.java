package com.sindoflow.ops.agentinfra.llm;

import java.util.List;

public interface LlmProvider {

    LlmResponse chat(LlmRequest request, String apiKey);

    List<String> listModels(String apiKey);

    String getProviderName();
}
