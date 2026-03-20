package com.owlsburg.ops.agentinfra.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
import com.owlsburg.ops.agentinfra.llm.LlmToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles HTTP streaming communication with the Anthropic Messages API.
 * Extracted from CeoAgent for reusability and separation of concerns.
 */
public final class AnthropicStreamingClient {

    public record StreamResult(String text, String stopReason, List<ToolUseBlock> toolUses,
                               int inputTokens, int outputTokens) {}

    public record ToolUseBlock(String id, String name, String input) {}

    private static final Logger log = LoggerFactory.getLogger(AnthropicStreamingClient.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {1000, 3000, 8000};

    private final String apiKey;
    private final ObjectMapper objectMapper;

    public AnthropicStreamingClient(String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    /**
     * Streams a request to the Anthropic API, forwarding text tokens via SSE.
     * Returns a StreamResult with the full text, stop reason, tool uses, and token counts.
     */
    public StreamResult streamRequest(String model, String systemPrompt, List<ObjectNode> messages,
                                       List<LlmToolDefinition> tools, int maxTokens,
                                       Double temperature, SseEmitter emitter) throws Exception {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        body.put("system", systemPrompt);
        body.put("stream", true);

        ArrayNode messagesArray = body.putArray("messages");
        for (ObjectNode msg : messages) {
            messagesArray.add(msg);
        }

        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (int i = 0; i < tools.size(); i++) {
                LlmToolDefinition tool = tools.get(i);
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("name", tool.name());
                toolNode.put("description", tool.description());
                try {
                    JsonNode schema = objectMapper.readTree(tool.inputSchema());
                    // Ensure schema has "type" field
                    if (!schema.has("type")) {
                        ObjectNode schemaObj = (ObjectNode) schema;
                        schemaObj.put("type", "object");
                    }
                    toolNode.set("input_schema", schema);
                } catch (Exception e) {
                    log.warn("Invalid input_schema for tool '{}' (index {}): {}", tool.name(), i, e.getMessage());
                    ObjectNode emptySchema = objectMapper.createObjectNode();
                    emptySchema.put("type", "object");
                    emptySchema.putObject("properties");
                    toolNode.set("input_schema", emptySchema);
                }
            }
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<java.io.InputStream> response = sendWithRetry(httpRequest);

        StringBuilder textContent = new StringBuilder();
        List<ToolUseBlock> toolUses = new ArrayList<>();
        String stopReason = "end_turn";
        int inputTokens = 0;
        int outputTokens = 0;

        String currentToolId = null;
        String currentToolName = null;
        StringBuilder currentToolInput = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) break;

                JsonNode event = objectMapper.readTree(data);
                String type = event.has("type") ? event.get("type").asText() : "";

                switch (type) {
                    case "message_start" -> {
                        JsonNode msg = event.get("message");
                        if (msg != null && msg.has("usage")) {
                            inputTokens = msg.get("usage").path("input_tokens").asInt(0);
                        }
                    }
                    case "content_block_start" -> {
                        JsonNode contentBlock = event.get("content_block");
                        if (contentBlock != null && "tool_use".equals(contentBlock.get("type").asText())) {
                            currentToolId = contentBlock.get("id").asText();
                            currentToolName = contentBlock.get("name").asText();
                            currentToolInput.setLength(0);
                        }
                    }
                    case "content_block_delta" -> {
                        JsonNode delta = event.get("delta");
                        if (delta == null) continue;

                        String deltaType = delta.get("type").asText();
                        if ("text_delta".equals(deltaType)) {
                            String token = delta.get("text").asText();
                            textContent.append(token);
                            if (!trySend(emitter, "{\"token\":" + objectMapper.writeValueAsString(token) + "}")) {
                                log.info("Client disconnected during streaming, aborting");
                                return new StreamResult(textContent.toString(), "client_disconnected",
                                        toolUses, inputTokens, outputTokens);
                            }
                        } else if ("input_json_delta".equals(deltaType)) {
                            String partialJson = delta.get("partial_json").asText();
                            currentToolInput.append(partialJson);
                        }
                    }
                    case "content_block_stop" -> {
                        if (currentToolId != null) {
                            String inputJson = currentToolInput.toString();
                            if (inputJson.isEmpty()) inputJson = "{}";
                            toolUses.add(new ToolUseBlock(currentToolId, currentToolName, inputJson));
                            currentToolId = null;
                            currentToolName = null;
                            currentToolInput.setLength(0);
                        }
                    }
                    case "message_delta" -> {
                        JsonNode delta = event.get("delta");
                        if (delta != null && delta.has("stop_reason")) {
                            stopReason = delta.get("stop_reason").asText();
                        }
                        JsonNode usage = event.get("usage");
                        if (usage != null) {
                            outputTokens = usage.path("output_tokens").asInt(0);
                        }
                    }
                }
            }
        }

        return new StreamResult(textContent.toString(), stopReason, toolUses, inputTokens, outputTokens);
    }

    /**
     * Attempts to send data via SSE. Returns false if client is disconnected.
     */
    public boolean trySend(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
            return true;
        } catch (Exception e) {
            log.debug("SSE send failed (client disconnected?): {}", e.getMessage());
            return false;
        }
    }

    private HttpResponse<java.io.InputStream> sendWithRetry(HttpRequest httpRequest) throws Exception {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            HttpResponse<java.io.InputStream> response = HTTP_CLIENT.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                return response;
            }

            String errorBody;
            try (java.io.InputStream errStream = response.body()) {
                errorBody = new String(errStream.readAllBytes());
            }

            int status = response.statusCode();
            if (isRetryable(status) && attempt < MAX_RETRIES) {
                log.warn("Anthropic streaming API returned {} (attempt {}/{}), retrying in {}ms...",
                        status, attempt + 1, MAX_RETRIES + 1, RETRY_DELAYS_MS[attempt]);
                try {
                    Thread.sleep(RETRY_DELAYS_MS[attempt]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LlmProviderException("Unterbrochen während Retry");
                }
                continue;
            }

            log.error("Anthropic API error ({}): {}", status, errorBody);
            throw new LlmProviderException("Anthropic API Fehler: HTTP " + status + " - " + errorBody);
        }
        throw new LlmProviderException("Anthropic API nicht erreichbar nach " + (MAX_RETRIES + 1) + " Versuchen");
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode == 529 || statusCode == 500
                || statusCode == 502 || statusCode == 503;
    }
}
