package com.owlsburg.ops.agentinfra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.dto.SimpleChatRequest;
import com.owlsburg.ops.agentinfra.llm.LlmConfigService;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
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

@Service
public class SimpleChatService {

    private static final Logger log = LoggerFactory.getLogger(SimpleChatService.class);

    private final AgentInstanceRepository agentInstanceRepository;
    private final AgentTemplateRepository agentTemplateRepository;
    private final LlmConfigService llmConfigService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SimpleChatService(AgentInstanceRepository agentInstanceRepository,
                             AgentTemplateRepository agentTemplateRepository,
                             LlmConfigService llmConfigService,
                             ObjectMapper objectMapper) {
        this.agentInstanceRepository = agentInstanceRepository;
        this.agentTemplateRepository = agentTemplateRepository;
        this.llmConfigService = llmConfigService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void streamChat(SimpleChatRequest request, SseEmitter emitter) {
        try {
            // 1. Load agent instance
            AgentInstanceEntity instance = agentInstanceRepository.findById(request.agentInstanceId())
                    .orElseThrow(() -> new IllegalArgumentException("Agent nicht gefunden"));

            // 2. Load agent template
            AgentTemplateEntity template = agentTemplateRepository.findById(instance.getTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Agent-Template nicht gefunden"));

            // 3. Build system prompt
            String systemPrompt = buildSystemPrompt(template.getBasePrompt());

            // 4. Get API key and model
            String apiKey = llmConfigService.getDecryptedApiKey();
            String model = llmConfigService.getConfig()
                    .map(c -> c.getDefaultModel())
                    .orElse("claude-sonnet-4-20250514");

            // 5. Build request body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 2048);
            body.put("system", systemPrompt);
            body.put("stream", true);

            ArrayNode messagesArray = body.putArray("messages");

            // Add history if present
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

            // 6. Call Anthropic API with streaming
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

            // 7. Parse SSE stream and forward tokens
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
                            emitter.send(SseEmitter.event().data(
                                    "{\"token\":" + objectMapper.writeValueAsString(token) + "}"));
                        }
                    }
                }
            }

            // 8. Send done event
            emitter.send(SseEmitter.event().data("{\"done\":true}"));
            emitter.complete();

        } catch (LlmProviderException e) {
            sendErrorAndComplete(emitter, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendErrorAndComplete(emitter, e.getMessage());
        } catch (Exception e) {
            log.error("Chat streaming error", e);
            sendErrorAndComplete(emitter, "Interner Fehler");
        }
    }

    private String buildSystemPrompt(String basePrompt) {
        String base = (basePrompt != null && !basePrompt.isBlank()) ? basePrompt : "Du bist ein hilfreicher Assistent.";
        return base + "\n\n## Kontext\n" +
                "- Datum: " + LocalDate.now() + "\n" +
                "- Du antwortest in einem Chat-Fenster. Halte deine Antworten knapp und hilfreich.\n" +
                "- Du hast aktuell KEINE Tools zur Verfügung. Wenn der Nutzer nach konkreten Daten fragt, " +
                "weise darauf hin dass du im Chat-Modus keine Datenbank-Abfragen machen kannst.\n\n" +
                "## Regeln\n" +
                "- Antworte immer auf Deutsch.\n" +
                "- Halte deine Antworten präzise und handlungsorientiert.\n";
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
