package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.systemagent.dto.SystemChatMessageResponse;
import com.owlsburg.ops.systemagent.dto.SystemChatRequest;
import com.owlsburg.ops.systemagent.dto.SystemChatSessionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/chat")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemChatController {

    private static final Logger log = LoggerFactory.getLogger(SystemChatController.class);

    private final SystemChatService chatService;
    private final SystemChatSessionService chatSessionService;

    public SystemChatController(SystemChatService chatService,
                                 SystemChatSessionService chatSessionService) {
        this.chatService = chatService;
        this.chatSessionService = chatSessionService;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SystemChatSessionResponse>> getSessions() {
        UUID userId = getCurrentUserId();
        return ApiResponse.ok(chatSessionService.getUserSessions(userId));
    }

    @PostMapping("/sessions")
    public ApiResponse<SystemChatSessionResponse> createSession(@RequestBody CreateSystemSessionRequest request) {
        UUID userId = getCurrentUserId();
        SystemChatSessionEntity session = chatSessionService.createSession(userId, request.agentInstanceId());
        return ApiResponse.ok(SystemChatSessionResponse.from(session));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        chatSessionService.deleteSession(id, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<SystemChatMessageResponse>> getMessages(@PathVariable UUID id) {
        return ApiResponse.ok(chatSessionService.getMessages(id));
    }

    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@RequestBody SystemChatRequest request) {
        UUID userId = getCurrentUserId();
        SseEmitter emitter = new SseEmitter(120_000L);

        // No TenantContext needed for system agents
        Thread.startVirtualThread(() -> {
            try {
                chatService.streamChat(request, userId, emitter);
            } catch (Exception e) {
                log.error("Unexpected error in system chat stream thread", e);
                try {
                    emitter.send(SseEmitter.event().data("{\"error\":\"Interner Fehler\"}"));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    private UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) auth.getPrincipal();
    }

    public record CreateSystemSessionRequest(UUID agentInstanceId) {}
}
