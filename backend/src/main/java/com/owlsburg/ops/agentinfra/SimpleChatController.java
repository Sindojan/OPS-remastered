package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.ChatMessageResponse;
import com.owlsburg.ops.agentinfra.dto.ChatSessionResponse;
import com.owlsburg.ops.agentinfra.dto.CreateSessionRequest;
import com.owlsburg.ops.agentinfra.dto.SimpleChatRequest;
import com.owlsburg.ops.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import com.owlsburg.ops.common.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class SimpleChatController {

    private static final Logger log = LoggerFactory.getLogger(SimpleChatController.class);

    private final SimpleChatService simpleChatService;
    private final ChatSessionService chatSessionService;

    public SimpleChatController(SimpleChatService simpleChatService,
                                ChatSessionService chatSessionService) {
        this.simpleChatService = simpleChatService;
        this.chatSessionService = chatSessionService;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionResponse>> getSessions() {
        UUID userId = getCurrentUserId();
        return ApiResponse.ok(chatSessionService.getUserSessions(userId));
    }

    @PostMapping("/sessions")
    public ApiResponse<ChatSessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        UUID userId = getCurrentUserId();
        ChatSessionEntity session = chatSessionService.createSession(userId, request.agentInstanceId());
        return ApiResponse.ok(ChatSessionResponse.from(session));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        chatSessionService.deleteSession(id, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessageResponse>> getMessages(@PathVariable UUID id) {
        return ApiResponse.ok(chatSessionService.getMessages(id));
    }

    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@RequestBody SimpleChatRequest request) {
        UUID userId = getCurrentUserId();
        String tenantId = TenantContext.getCurrentTenant();
        SseEmitter emitter = new SseEmitter(120_000L);

        Thread.startVirtualThread(() -> {
            try {
                TenantContext.setCurrentTenant(tenantId);
                simpleChatService.streamChat(request, userId, emitter);
            } catch (Exception e) {
                log.error("Chat streaming error: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().data("{\"error\":\"Ein interner Fehler ist aufgetreten.\"}"));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                TenantContext.clear();
            }
        });

        return emitter;
    }

    private UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) auth.getPrincipal();
    }
}
