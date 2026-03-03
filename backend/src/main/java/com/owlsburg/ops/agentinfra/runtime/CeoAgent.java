package com.owlsburg.ops.agentinfra.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
import com.owlsburg.ops.agentinfra.llm.LlmToolDefinition;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.agentinfra.tools.ToolExecutionContext;
import com.owlsburg.ops.agentinfra.tools.ToolResult;
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
import java.util.Map;

public final class CeoAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(CeoAgent.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final AgentIdentity identity;
    private final AgentCapabilities capabilities;
    private final String apiKey;
    private final AgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public CeoAgent(AgentIdentity identity, AgentCapabilities capabilities,
                    String apiKey, AgentToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.identity = identity;
        this.capabilities = capabilities;
        this.apiKey = apiKey;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentIdentity identity() {
        return identity;
    }

    @Override
    public AgentCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public AgentResult execute(AgentContext context, String task) {
        // CEO streamt immer — Fallback für nicht-streaming Aufrufer
        try {
            SseEmitter dummy = new SseEmitter(300_000L);
            dummy.onTimeout(dummy::complete);
            dummy.onError(e -> dummy.complete());
            StringBuilder result = new StringBuilder();
            executeStreamingInternal(context, task, List.of(), dummy, result);
            return AgentResult.completed(result.toString(), 0, 0, List.of());
        } catch (Exception e) {
            return AgentResult.error("CEO execution error: " + e.getMessage());
        }
    }

    @Override
    public void executeStreaming(AgentContext context, String task, SseEmitter emitter) {
        executeStreamingWithHistory(context, task, List.of(), emitter);
    }

    /**
     * Streaming execution with pre-loaded chat history (as ObjectNode messages).
     */
    public void executeStreamingWithHistory(AgentContext context, String task,
                                             List<ObjectNode> chatHistory, SseEmitter emitter) {
        StringBuilder fullResponse = new StringBuilder();
        try {
            executeStreamingInternal(context, task, chatHistory, emitter, fullResponse);
        } catch (LlmProviderException e) {
            sendErrorEvent(emitter, e.getMessage());
        } catch (Exception e) {
            log.error("CEO streaming error", e);
            sendErrorEvent(emitter, "Interner Fehler");
        }
    }

    /**
     * Returns the accumulated full response text.
     */
    public String getLastResponse(AgentContext context, String task,
                                   List<ObjectNode> chatHistory, SseEmitter emitter) {
        StringBuilder fullResponse = new StringBuilder();
        try {
            executeStreamingInternal(context, task, chatHistory, emitter, fullResponse);
        } catch (Exception e) {
            log.error("CEO streaming error", e);
            sendErrorEvent(emitter, "Interner Fehler");
        }
        return fullResponse.toString();
    }

    private void executeStreamingInternal(AgentContext context, String task,
                                           List<ObjectNode> chatHistory, SseEmitter emitter,
                                           StringBuilder fullResponse) throws Exception {
        List<ObjectNode> messages = new ArrayList<>(chatHistory);

        ToolExecutionContext toolContext = new ToolExecutionContext(
                context.tenantId(), identity.instanceId(), null);

        for (int iteration = 0; iteration < capabilities.maxIterations(); iteration++) {
            StreamResult streamResult = streamAnthropicRequest(
                    identity.systemPrompt(), messages, capabilities.toolDefinitions(), emitter);

            fullResponse.append(streamResult.text);

            if (!"tool_use".equals(streamResult.stopReason) || streamResult.toolUses.isEmpty()) {
                break;
            }

            // Build assistant message with all content blocks
            ObjectNode assistantMsg = objectMapper.createObjectNode();
            assistantMsg.put("role", "assistant");
            ArrayNode contentBlocks = assistantMsg.putArray("content");

            if (!streamResult.text.isEmpty()) {
                ObjectNode textBlock = contentBlocks.addObject();
                textBlock.put("type", "text");
                textBlock.put("text", streamResult.text);
            }

            for (ToolUseBlock toolUse : streamResult.toolUses) {
                ObjectNode toolUseBlock = contentBlocks.addObject();
                toolUseBlock.put("type", "tool_use");
                toolUseBlock.put("id", toolUse.id);
                toolUseBlock.put("name", toolUse.name);
                try {
                    toolUseBlock.set("input", objectMapper.readTree(toolUse.input));
                } catch (Exception e) {
                    toolUseBlock.putObject("input");
                }
            }
            messages.add(assistantMsg);

            // Execute tools and build tool result message
            ObjectNode toolResultMsg = objectMapper.createObjectNode();
            toolResultMsg.put("role", "user");
            ArrayNode toolResultBlocks = toolResultMsg.putArray("content");

            for (ToolUseBlock toolUse : streamResult.toolUses) {
                // Send delegation/toolCall SSE events
                if ("delegate_to_lead".equals(toolUse.name)) {
                    try {
                        JsonNode delegateInput = objectMapper.readTree(toolUse.input);
                        String leadName = delegateInput.has("lead") ? delegateInput.get("lead").asText() : "unknown";
                        String delegateTask = delegateInput.has("task") ? delegateInput.get("task").asText() : "";
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                                Map.of("delegation", Map.of("lead", leadName, "task", delegateTask, "status", "running")))));
                    } catch (Exception e) {
                        log.debug("SSE delegation event send failed: {}", e.getMessage());
                    }
                } else {
                    try {
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                                Map.of("toolCall", Map.of("name", toolUse.name, "input", toolUse.input)))));
                    } catch (Exception e) {
                        log.debug("SSE toolCall event send failed: {}", e.getMessage());
                    }
                }

                // Execute tool
                String toolResultContent;
                try {
                    AgentTool tool = toolRegistry.getTool(toolUse.name)
                            .orElseThrow(() -> new IllegalArgumentException("Tool nicht gefunden: " + toolUse.name));
                    ToolResult result = tool.execute(toolContext, toolUse.input);
                    toolResultContent = result.success() ? result.data() : "Fehler: " + result.errorMessage();
                } catch (Exception e) {
                    log.error("Tool execution error for '{}': {}", toolUse.name, e.getMessage());
                    toolResultContent = "Fehler bei Tool-Ausführung: " + e.getMessage();
                }

                // Send result SSE events
                if ("delegate_to_lead".equals(toolUse.name)) {
                    try {
                        JsonNode delegateInput = objectMapper.readTree(toolUse.input);
                        String leadName = delegateInput.has("lead") ? delegateInput.get("lead").asText() : "unknown";
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                                Map.of("delegationResult", Map.of("lead", leadName, "result", toolResultContent)))));
                    } catch (Exception e) {
                        log.debug("SSE delegationResult event send failed: {}", e.getMessage());
                    }
                } else {
                    try {
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                                Map.of("toolResult", Map.of("name", toolUse.name, "result", toolResultContent)))));
                    } catch (Exception e) {
                        log.debug("SSE toolResult event send failed: {}", e.getMessage());
                    }
                }

                // Add to tool result message
                ObjectNode resultBlock = toolResultBlocks.addObject();
                resultBlock.put("type", "tool_result");
                resultBlock.put("tool_use_id", toolUse.id);
                resultBlock.put("content", toolResultContent);
            }
            messages.add(toolResultMsg);
        }
    }

    private StreamResult streamAnthropicRequest(
            String systemPrompt, List<ObjectNode> messages,
            List<LlmToolDefinition> tools, SseEmitter emitter) throws Exception {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", identity.model());
        body.put("max_tokens", capabilities.maxTokensPerRun());
        body.put("system", systemPrompt);
        body.put("stream", true);

        ArrayNode messagesArray = body.putArray("messages");
        for (ObjectNode msg : messages) {
            messagesArray.add(msg);
        }

        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (LlmToolDefinition tool : tools) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("name", tool.name());
                toolNode.put("description", tool.description());
                try {
                    toolNode.set("input_schema", objectMapper.readTree(tool.inputSchema()));
                } catch (Exception e) {
                    toolNode.putObject("input_schema");
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

        HttpResponse<java.io.InputStream> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            try (java.io.InputStream errStream = response.body()) {
                String errorBody = new String(errStream.readAllBytes());
                log.error("Anthropic API error ({}): {}", response.statusCode(), errorBody);
            }
            throw new LlmProviderException("Anthropic API Fehler: HTTP " + response.statusCode());
        }

        StringBuilder textContent = new StringBuilder();
        List<ToolUseBlock> toolUses = new ArrayList<>();
        String stopReason = "end_turn";

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
                            emitter.send(SseEmitter.event().data(
                                    "{\"token\":" + objectMapper.writeValueAsString(token) + "}"));
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
                    }
                }
            }
        }

        return new StreamResult(textContent.toString(), stopReason, toolUses);
    }

    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(
                    "{\"error\":" + objectMapper.writeValueAsString(message) + "}"));
        } catch (Exception e) {
            log.debug("SSE error event send failed: {}", e.getMessage());
        }
    }

    private record StreamResult(String text, String stopReason, List<ToolUseBlock> toolUses) {}
    private record ToolUseBlock(String id, String name, String input) {}
}
