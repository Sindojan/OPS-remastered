package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.systemagent.dto.SystemChatMessageResponse;
import com.owlsburg.ops.systemagent.dto.SystemChatSessionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SystemChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(SystemChatSessionService.class);

    private final SystemChatSessionRepository chatSessionRepository;
    private final SystemChatMessageRepository chatMessageRepository;

    public SystemChatSessionService(SystemChatSessionRepository chatSessionRepository,
                                     SystemChatMessageRepository chatMessageRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional(readOnly = true)
    public List<SystemChatSessionResponse> getUserSessions(UUID userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream().map(SystemChatSessionResponse::from).toList();
    }

    @Transactional
    public SystemChatSessionEntity createSession(UUID userId, UUID agentInstanceId) {
        SystemChatSessionEntity session = new SystemChatSessionEntity();
        session.setUserId(userId);
        session.setAgentInstanceId(agentInstanceId);
        return chatSessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        SystemChatSessionEntity session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session nicht gefunden"));
        if (!session.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Zugriff verweigert");
        }
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<SystemChatMessageResponse> getMessages(UUID sessionId) {
        chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session nicht gefunden"));
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(SystemChatMessageResponse::from).toList();
    }

    @Transactional
    public SystemChatMessageEntity saveMessage(UUID sessionId, String role, String content) {
        SystemChatMessageEntity msg = new SystemChatMessageEntity();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        return chatMessageRepository.save(msg);
    }

    @Transactional
    public void updateSessionTitle(UUID sessionId, String title) {
        chatSessionRepository.findById(sessionId).ifPresent(s -> {
            s.setTitle(title);
            chatSessionRepository.save(s);
        });
    }
}
