package com.owlsburg.ops.agentinfra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.dto.ChatMessageResponse;
import com.owlsburg.ops.agentinfra.dto.SimpleChatRequest;
import com.owlsburg.ops.agentinfra.llm.LlmConfigService;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
import com.owlsburg.ops.common.TenantContext;
import com.owlsburg.ops.tenant.TenantRepository;
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
import java.util.List;
import java.util.UUID;

@Service
public class SimpleChatService {

    private static final Logger log = LoggerFactory.getLogger(SimpleChatService.class);

    private final AgentInstanceRepository agentInstanceRepository;
    private final AgentTemplateRepository agentTemplateRepository;
    private final LlmConfigService llmConfigService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ChatSessionService chatSessionService;
    private final TenantRepository tenantRepository;

    public SimpleChatService(AgentInstanceRepository agentInstanceRepository,
                             AgentTemplateRepository agentTemplateRepository,
                             LlmConfigService llmConfigService,
                             ObjectMapper objectMapper,
                             ChatSessionService chatSessionService,
                             TenantRepository tenantRepository) {
        this.agentInstanceRepository = agentInstanceRepository;
        this.agentTemplateRepository = agentTemplateRepository;
        this.llmConfigService = llmConfigService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.chatSessionService = chatSessionService;
        this.tenantRepository = tenantRepository;
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

            // 2. Save user message to DB
            chatSessionService.saveMessage(sessionId, "user", request.message());

            // 3. Send sessionId as first SSE event
            emitter.send(SseEmitter.event().data("{\"sessionId\":\"" + sessionId + "\"}"));

            // 4. Load agent instance
            AgentInstanceEntity instance = agentInstanceRepository.findById(request.agentInstanceId())
                    .orElseThrow(() -> new IllegalArgumentException("Agent nicht gefunden"));

            // 5. Load agent template
            AgentTemplateEntity template = agentTemplateRepository.findById(instance.getTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Agent-Template nicht gefunden"));

            // 6. Build system prompt with tenant name resolution
            String tenantName = resolveTenantName();
            String systemPrompt = buildSystemPrompt(template.getBasePrompt(), tenantName);

            // 7. Get API key and model
            String apiKey = llmConfigService.getDecryptedApiKey();
            String model = llmConfigService.getConfig()
                    .map(c -> c.getDefaultModel())
                    .orElse("claude-sonnet-4-20250514");

            // 8. Build request body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 2048);
            body.put("system", systemPrompt);
            body.put("stream", true);

            ArrayNode messagesArray = body.putArray("messages");

            // Load history from DB if session already existed, otherwise from request
            if (request.sessionId() != null) {
                List<ChatMessageResponse> dbHistory = chatSessionService.getMessages(request.sessionId());
                for (ChatMessageResponse msg : dbHistory) {
                    // Skip the current user message we just saved (it's the last one)
                    ObjectNode msgNode = messagesArray.addObject();
                    msgNode.put("role", msg.role());
                    msgNode.put("content", msg.content());
                }
            } else {
                // New session: add any provided history
                if (request.history() != null) {
                    for (SimpleChatRequest.ChatHistoryMessage msg : request.history()) {
                        ObjectNode msgNode = messagesArray.addObject();
                        msgNode.put("role", msg.role());
                        msgNode.put("content", msg.content());
                    }
                }
                // Add current message
                ObjectNode currentMsg = messagesArray.addObject();
                currentMsg.put("role", "user");
                currentMsg.put("content", request.message());
            }

            // 9. Call Anthropic API with streaming
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

            // 10. Parse SSE stream and forward tokens
            StringBuilder fullResponse = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    JsonNode event = objectMapper.readTree(data);
                    String type = event.has("type") ? event.get("type").asText() : "";

                    if ("content_block_delta".equals(type)) {
                        JsonNode delta = event.get("delta");
                        if (delta != null && "text_delta".equals(delta.get("type").asText())) {
                            String token = delta.get("text").asText();
                            fullResponse.append(token);
                            emitter.send(SseEmitter.event().data(
                                    "{\"token\":" + objectMapper.writeValueAsString(token) + "}"));
                        }
                    }
                }
            }

            // 11. Save assistant message to DB
            chatSessionService.saveMessage(sessionId, "assistant", fullResponse.toString());

            // 12. Auto-generate title on first message (new session)
            if (request.sessionId() == null) {
                String title = request.message().length() > 50
                        ? request.message().substring(0, 50) + "..."
                        : request.message();
                chatSessionService.updateSessionTitle(sessionId, title);
            }

            // 13. Send done event
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
}
