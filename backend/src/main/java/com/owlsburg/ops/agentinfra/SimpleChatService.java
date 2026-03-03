package com.owlsburg.ops.agentinfra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.dto.ChatMessageResponse;
import com.owlsburg.ops.agentinfra.dto.SimpleChatRequest;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
import com.owlsburg.ops.agentinfra.runtime.*;
import com.owlsburg.ops.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SimpleChatService {

    private static final Logger log = LoggerFactory.getLogger(SimpleChatService.class);

    private final AgentInstanceRepository agentInstanceRepository;
    private final ChatSessionService chatSessionService;
    private final AgentFactory agentFactory;
    private final ObjectMapper objectMapper;

    public SimpleChatService(AgentInstanceRepository agentInstanceRepository,
                             ChatSessionService chatSessionService,
                             AgentFactory agentFactory,
                             ObjectMapper objectMapper) {
        this.agentInstanceRepository = agentInstanceRepository;
        this.chatSessionService = chatSessionService;
        this.agentFactory = agentFactory;
        this.objectMapper = objectMapper;
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
            String tenantId = TenantContext.getCurrentTenant();
            UUID tenantUuid = UUID.fromString(tenantId);
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

            // 6. Create Agent via Factory (builds system prompt, tools, memory injection)
            Agent agent = agentFactory.createAgent(request.agentInstanceId(), tenantId);

            // 7. Load full history from DB and build messages
            List<ChatMessageResponse> dbHistory = chatSessionService.getMessages(sessionId);
            List<ObjectNode> messages = new ArrayList<>();
            for (ChatMessageResponse msg : dbHistory) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
                messages.add(msgNode);
            }

            // 8. Execute streaming via CeoAgent
            AgentContext context = AgentContext.forChat(tenantId, userId, sessionId);

            String fullResponse;
            if (agent instanceof CeoAgent ceoAgent) {
                fullResponse = ceoAgent.getLastResponse(context, request.message(), messages, emitter);
            } else {
                // Non-CEO agents: execute synchronously and send result as token
                AgentResult result = agent.execute(context, request.message());
                fullResponse = result.output();
                if (fullResponse != null && !fullResponse.isEmpty()) {
                    emitter.send(SseEmitter.event().data(
                            "{\"token\":" + objectMapper.writeValueAsString(fullResponse) + "}"));
                }
            }

            // 9. Save assistant message to DB
            if (fullResponse != null && !fullResponse.isEmpty()) {
                chatSessionService.saveMessage(sessionId, "assistant", fullResponse);
            }

            // 10. Auto-generate title on first message (new session)
            if (request.sessionId() == null) {
                String title = request.message().length() > 50
                        ? request.message().substring(0, 50) + "..."
                        : request.message();
                chatSessionService.updateSessionTitle(sessionId, title);
            }

            // 11. Send done event
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
