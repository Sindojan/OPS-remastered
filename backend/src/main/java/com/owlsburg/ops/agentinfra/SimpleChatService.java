package com.owlsburg.ops.agentinfra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.dto.ChatMessageResponse;
import com.owlsburg.ops.agentinfra.dto.SimpleChatRequest;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
import com.owlsburg.ops.agentinfra.llm.LlmConfigService;
import com.owlsburg.ops.agentinfra.llm.LlmToolDefinition;
import com.owlsburg.ops.agentinfra.tools.AgentTool;
import com.owlsburg.ops.agentinfra.tools.AgentToolRegistry;
import com.owlsburg.ops.agentinfra.tools.ToolExecutionContext;
import com.owlsburg.ops.agentinfra.tools.ToolResult;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.tenant.TenantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SimpleChatService {

    private static final Logger log = LoggerFactory.getLogger(SimpleChatService.class);
    private static final int MAX_TOOL_ITERATIONS = 10;

    private final AgentInstanceRepository agentInstanceRepository;
    private final AgentTemplateRepository agentTemplateRepository;
    private final LlmConfigService llmConfigService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ChatSessionService chatSessionService;
    private final TenantRepository tenantRepository;
    private final AgentToolRegistry toolRegistry;

    public SimpleChatService(AgentInstanceRepository agentInstanceRepository,
                             AgentTemplateRepository agentTemplateRepository,
                             LlmConfigService llmConfigService,
                             ObjectMapper objectMapper,
                             ChatSessionService chatSessionService,
                             TenantRepository tenantRepository,
                             AgentToolRegistry toolRegistry) {
        this.agentInstanceRepository = agentInstanceRepository;
        this.agentTemplateRepository = agentTemplateRepository;
        this.llmConfigService = llmConfigService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.chatSessionService = chatSessionService;
        this.tenantRepository = tenantRepository;
        this.toolRegistry = toolRegistry;
    }

    public UUID streamChat(SimpleChatRequest request, UUID userId, SseEmitter emitter) {
        UUID sessionId = null;
        try {
            // 1. Resolve or create session
            if (request.sessionId() != null) {
                sessionId = request.sessionId();
            } else {
                ChatSessionEntity session = chatSessionService.createSession(userId, request.agentInstanceId());
                sessionId = session.getId();
            }

            // 2. Load agent instance with tenant check (defense-in-depth)
            UUID tenantUuid = UUID.fromString(TenantContext.getCurrentTenant());
            AgentInstanceEntity instance = agentInstanceRepository.findByIdAndTenantId(request.agentInstanceId(), tenantUuid)
                    .orElseThrow(() -> new AccessDeniedException("Zugriff verweigert"));

            // 3. Save greeting on new session
            if (request.sessionId() == null) {
                String greeting = "Guten Tag! Ich bin Ihr " + instance.getName() + ". Wie kann ich Ihnen helfen?";
                chatSessionService.saveMessage(sessionId, "assistant", greeting);
            }

            // 4. Save user message to DB
            chatSessionService.saveMessage(sessionId, "user", request.message());

            // 5. Send sessionId as first SSE event
            emitter.send(SseEmitter.event().data("{\"sessionId\":\"" + sessionId + "\"}"));

            // 6. Load agent template with tenant check
            AgentTemplateEntity template = agentTemplateRepository.findByIdAndTenantId(instance.getTemplateId(), tenantUuid)
                    .orElseThrow(() -> new AccessDeniedException("Zugriff verweigert"));

            // 7. Build system prompt – instance override takes precedence over template
            String basePrompt = (instance.getCustomSystemPrompt() != null && !instance.getCustomSystemPrompt().isBlank())
                    ? instance.getCustomSystemPrompt()
                    : template.getBasePrompt();
            String tenantName = resolveTenantName();
            String systemPrompt = buildSystemPrompt(basePrompt, tenantName);

            // 8. Get API key and model (per-instance override → tenant default → fallback)
            String apiKey = llmConfigService.getDecryptedApiKey();
            String model = resolveModel(instance);

            // 9. Get tools for this agent template
            List<AgentTool> agentTools = toolRegistry.getToolsForInstance(template);
            List<LlmToolDefinition> toolDefs = agentTools.stream()
                    .map(t -> new LlmToolDefinition(t.getName(), t.getDescription(), t.getInputSchema()))
                    .toList();

            // 10. Load full history from DB and build messages
            List<ChatMessageResponse> dbHistory = chatSessionService.getMessages(sessionId);
            List<ObjectNode> messages = new ArrayList<>();
            for (ChatMessageResponse msg : dbHistory) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
                messages.add(msgNode);
            }

            // 11. ReAct loop – stream with tool calling
            StringBuilder fullResponse = new StringBuilder();
            ToolExecutionContext toolContext = new ToolExecutionContext(
                    TenantContext.getCurrentTenant(), instance.getId(), null);

            for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
                StreamResult streamResult = streamAnthropicRequest(
                        apiKey, model, systemPrompt, messages, toolDefs, emitter);

                fullResponse.append(streamResult.text);

                if (!"tool_use".equals(streamResult.stopReason) || streamResult.toolUses.isEmpty()) {
                    // No more tool calls – we're done
                    break;
                }

                // Process each tool call in this response
                // Build assistant message with all content blocks
                ObjectNode assistantMsg = objectMapper.createObjectNode();
                assistantMsg.put("role", "assistant");
                ArrayNode contentBlocks = assistantMsg.putArray("content");

                // Add text block if present
                if (!streamResult.text.isEmpty()) {
                    ObjectNode textBlock = contentBlocks.addObject();
                    textBlock.put("type", "text");
                    textBlock.put("text", streamResult.text);
                }

                // Add all tool_use blocks
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
                    // Send delegation event before executing delegate_to_lead
                    if ("delegate_to_lead".equals(toolUse.name)) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode delegateInput = objectMapper.readTree(toolUse.input);
                            String leadName = delegateInput.has("lead") ? delegateInput.get("lead").asText() : "unknown";
                            String delegateTask = delegateInput.has("task") ? delegateInput.get("task").asText() : "";
                            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                                    Map.of("delegation", Map.of("lead", leadName, "task", delegateTask, "status", "running")))));
                        } catch (Exception ignored) {}
                    } else {
                        // Send toolCall event to client (skip for delegate_to_lead, which has its own events)
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                                Map.of("toolCall", Map.of("name", toolUse.name, "input", toolUse.input)))));
                    }

                    // Execute the tool
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

                    // Send delegation result event after delegate_to_lead completes
                    if ("delegate_to_lead".equals(toolUse.name)) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode delegateInput = objectMapper.readTree(toolUse.input);
                            String leadName = delegateInput.has("lead") ? delegateInput.get("lead").asText() : "unknown";
                            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                                    Map.of("delegationResult", Map.of("lead", leadName, "result", toolResultContent)))));
                        } catch (Exception ignored) {}
                    }

                    // Send toolResult event to client (skip for delegate_to_lead, which has its own events)
                    if (!"delegate_to_lead".equals(toolUse.name)) {
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                                Map.of("toolResult", Map.of("name", toolUse.name, "result", toolResultContent)))));
                    }

                    // Add to tool result message
                    ObjectNode resultBlock = toolResultBlocks.addObject();
                    resultBlock.put("type", "tool_result");
                    resultBlock.put("tool_use_id", toolUse.id);
                    resultBlock.put("content", toolResultContent);
                }
                messages.add(toolResultMsg);
            }

            // 12. Save assistant message to DB
            if (!fullResponse.isEmpty()) {
                chatSessionService.saveMessage(sessionId, "assistant", fullResponse.toString());
            }

            // 13. Auto-generate title on first message (new session)
            if (request.sessionId() == null) {
                String title = request.message().length() > 50
                        ? request.message().substring(0, 50) + "..."
                        : request.message();
                chatSessionService.updateSessionTitle(sessionId, title);
            }

            // 14. Send done event
            emitter.send(SseEmitter.event().data("{\"done\":true}"));
            emitter.complete();

            return sessionId;

        } catch (LlmProviderException e) {
            sendErrorAndComplete(emitter, e.getMessage());
            return sessionId;
        } catch (IllegalArgumentException e) {
            sendErrorAndComplete(emitter, e.getMessage());
            return sessionId;
        } catch (Exception e) {
            log.error("Chat streaming error", e);
            sendErrorAndComplete(emitter, "Interner Fehler");
            return sessionId;
        }
    }

    /**
     * Streams a single Anthropic API request and returns the accumulated result.
     * Handles both text_delta and tool_use content blocks in the streaming response.
     */
    private StreamResult streamAnthropicRequest(
            String apiKey, String model, String systemPrompt,
            List<ObjectNode> messages, List<LlmToolDefinition> tools,
            SseEmitter emitter) throws Exception {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("system", systemPrompt);
        body.put("stream", true);

        // Messages
        ArrayNode messagesArray = body.putArray("messages");
        for (ObjectNode msg : messages) {
            messagesArray.add(msg);
        }

        // Tools
        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (LlmToolDefinition tool : tools) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("name", tool.name());
                toolNode.put("description", tool.description());
                try {
                    toolNode.set("input_schema", objectMapper.readTree(tool.inputSchema()));
                } catch (Exception e) {
                    log.warn("Failed to parse input_schema for tool '{}': {}", tool.name(), e.getMessage());
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

        HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes());
            log.error("Anthropic API error ({}): {}", response.statusCode(), errorBody);
            throw new LlmProviderException("Anthropic API Fehler: HTTP " + response.statusCode());
        }

        // Parse SSE stream
        StringBuilder textContent = new StringBuilder();
        List<ToolUseBlock> toolUses = new ArrayList<>();
        String stopReason = "end_turn";

        // Track current tool_use block being accumulated
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

    /**
     * Resolves the LLM model: instance config → tenant default → hardcoded fallback.
     */
    private String resolveModel(AgentInstanceEntity instance) {
        // 1. Try per-instance config
        if (instance.getConfig() != null && !instance.getConfig().equals("{}")) {
            try {
                com.fasterxml.jackson.databind.JsonNode configNode = objectMapper.readTree(instance.getConfig());
                if (configNode.has("model") && !configNode.get("model").asText().isBlank()) {
                    return configNode.get("model").asText();
                }
            } catch (Exception e) {
                log.warn("Failed to parse instance config for model: {}", e.getMessage());
            }
        }
        // 2. Tenant default
        return llmConfigService.getConfig()
                .map(c -> c.getDefaultModel())
                .orElse("claude-sonnet-4-6");
    }

    private String buildSystemPrompt(String basePrompt, String tenantName) {
        String base = (basePrompt != null && !basePrompt.isBlank()) ? basePrompt : "Du bist ein hilfreicher Assistent.";
        String resolved = base.replace("{{TENANT_NAME}}", tenantName);
        return resolved + "\n\n## Kontext\n- Datum: " + LocalDate.now() + "\n";
    }

    private String resolveTenantName() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            try {
                return tenantRepository.findById(UUID.fromString(tenantId))
                        .map(t -> t.getName())
                        .orElse("Owlsburg OPS");
            } catch (IllegalArgumentException e) {
                log.warn("Invalid tenant ID format: {}", tenantId);
                return "Owlsburg OPS";
            }
        }
        return "Owlsburg OPS";
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(
                    "{\"error\":" + objectMapper.writeValueAsString(message) + "}"));
            emitter.complete();
        } catch (Exception ex) {
            log.warn("Failed to send error event: {}", ex.getMessage());
            try {
                emitter.completeWithError(ex);
            } catch (Exception ignored) {
                // emitter already completed
            }
        }
    }

    // Internal data structures for streaming

    private record StreamResult(String text, String stopReason, List<ToolUseBlock> toolUses) {}

    private record ToolUseBlock(String id, String name, String input) {}
}
