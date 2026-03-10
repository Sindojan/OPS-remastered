package com.owlsburg.ops.agentinfra.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnthropicLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmProvider.class);

    private static final String MESSAGES_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODELS_URL = "https://api.anthropic.com/v1/models";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Cache: apiKey hash -> (timestamp, models)
    private final ConcurrentHashMap<Integer, CachedModels> modelsCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600_000; // 1 hour
    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {1000, 3000, 8000}; // Exponential backoff

    public AnthropicLlmProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    @Override
    public LlmResponse chat(LlmRequest request, String apiKey) {
        HttpHeaders headers = buildHeaders(apiKey);
        ObjectNode body = buildRequestBody(request);

        log.debug("Sending chat request to Anthropic API, model: {}", request.model());

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpEntity<String> httpEntity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
                ResponseEntity<String> response = restTemplate.exchange(
                        MESSAGES_URL, HttpMethod.POST, httpEntity, String.class
                );

                return parseResponse(response.getBody());
            } catch (HttpServerErrorException e) {
                int status = e.getStatusCode().value();
                if (isRetryable(status) && attempt < MAX_RETRIES) {
                    log.warn("Anthropic API returned {} (attempt {}/{}), retrying in {}ms...",
                            status, attempt + 1, MAX_RETRIES + 1, RETRY_DELAYS_MS[attempt]);
                    sleep(RETRY_DELAYS_MS[attempt]);
                    continue;
                }
                log.error("Anthropic API server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new LlmProviderException(
                        "Anthropic API error: " + e.getResponseBodyAsString(),
                        status, e);
            } catch (HttpClientErrorException e) {
                int status = e.getStatusCode().value();
                if (status == 429 && attempt < MAX_RETRIES) {
                    log.warn("Anthropic API rate limited (429, attempt {}/{}), retrying in {}ms...",
                            attempt + 1, MAX_RETRIES + 1, RETRY_DELAYS_MS[attempt]);
                    sleep(RETRY_DELAYS_MS[attempt]);
                    continue;
                }
                log.error("Anthropic API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new LlmProviderException(
                        "Anthropic API error: " + e.getResponseBodyAsString(),
                        status, e);
            } catch (LlmProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to call Anthropic API", e);
                throw new LlmProviderException("Failed to call Anthropic API: " + e.getMessage(), e);
            }
        }
        throw new LlmProviderException("Anthropic API nicht erreichbar nach " + (MAX_RETRIES + 1) + " Versuchen");
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode == 529 || statusCode == 500
                || statusCode == 502 || statusCode == 503;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public List<String> listModels(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Collections.emptyList();
        }

        int cacheKey = apiKey.hashCode();
        CachedModels cached = modelsCache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            return cached.models;
        }

        try {
            HttpHeaders headers = buildHeaders(apiKey);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    MODELS_URL, HttpMethod.GET, httpEntity, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            List<String> models = new ArrayList<>();
            if (data != null && data.isArray()) {
                for (JsonNode model : data) {
                    JsonNode idNode = model.get("id");
                    if (idNode != null) {
                        models.add(idNode.asText());
                    }
                }
            }

            Collections.sort(models);
            modelsCache.put(cacheKey, new CachedModels(System.currentTimeMillis(), models));
            log.info("Fetched {} models from Anthropic API", models.size());
            return models;
        } catch (HttpClientErrorException e) {
            log.error("Failed to list Anthropic models: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LlmProviderException(
                    "Failed to list models: " + e.getResponseBodyAsString(),
                    e.getStatusCode().value(),
                    e
            );
        } catch (Exception e) {
            log.error("Failed to list Anthropic models", e);
            throw new LlmProviderException("Failed to list models: " + e.getMessage(), e);
        }
    }

    private HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);
        return headers;
    }

    private ObjectNode buildRequestBody(LlmRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", request.model());
        body.put("max_tokens", request.maxTokens());
        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            body.put("system", request.systemPrompt());
        }

        ArrayNode messagesArray = body.putArray("messages");
        for (LlmMessage msg : request.messages()) {
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.role());

            if (msg.toolUse() != null) {
                // Assistant message with tool_use
                ArrayNode contentArray = msgNode.putArray("content");
                ObjectNode toolUseNode = contentArray.addObject();
                toolUseNode.put("type", "tool_use");
                toolUseNode.put("id", msg.toolUse().id());
                toolUseNode.put("name", msg.toolUse().name());
                try {
                    toolUseNode.set("input", objectMapper.readTree(msg.toolUse().input()));
                } catch (Exception e) {
                    toolUseNode.put("input", msg.toolUse().input());
                }
            } else if (msg.toolResult() != null) {
                // User message with tool_result
                ArrayNode contentArray = msgNode.putArray("content");
                ObjectNode toolResultNode = contentArray.addObject();
                toolResultNode.put("type", "tool_result");
                toolResultNode.put("tool_use_id", msg.toolResult().toolUseId());
                toolResultNode.put("content", msg.toolResult().content());
            } else {
                // Regular text message
                msgNode.put("content", msg.content());
            }

            messagesArray.add(msgNode);
        }

        if (request.tools() != null && !request.tools().isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (LlmToolDefinition tool : request.tools()) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("name", tool.name());
                toolNode.put("description", tool.description());
                try {
                    toolNode.set("input_schema", objectMapper.readTree(tool.inputSchema()));
                } catch (Exception e) {
                    log.warn("Failed to parse input_schema for tool '{}', using raw string", tool.name());
                    toolNode.put("input_schema", tool.inputSchema());
                }
            }
        }

        return body;
    }

    private LlmResponse parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            String content = null;
            LlmToolUse toolUse = null;

            JsonNode contentArray = root.get("content");
            if (contentArray != null && contentArray.isArray()) {
                for (JsonNode block : contentArray) {
                    String type = block.get("type").asText();
                    if ("text".equals(type)) {
                        content = block.get("text").asText();
                    } else if ("tool_use".equals(type)) {
                        toolUse = new LlmToolUse(
                                block.get("id").asText(),
                                block.get("name").asText(),
                                objectMapper.writeValueAsString(block.get("input"))
                        );
                    }
                }
            }

            String stopReason = root.has("stop_reason") ? root.get("stop_reason").asText() : null;

            int inputTokens = 0;
            int outputTokens = 0;
            JsonNode usage = root.get("usage");
            if (usage != null) {
                inputTokens = usage.has("input_tokens") ? usage.get("input_tokens").asInt() : 0;
                outputTokens = usage.has("output_tokens") ? usage.get("output_tokens").asInt() : 0;
            }

            String model = root.has("model") ? root.get("model").asText() : null;

            return new LlmResponse(content, toolUse, stopReason, inputTokens, outputTokens, model);
        } catch (Exception e) {
            throw new LlmProviderException("Failed to parse Anthropic response: " + e.getMessage(), e);
        }
    }

    private record CachedModels(long timestamp, List<String> models) {}
}
