package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.ChatMessageResponse;
import com.owlsburg.ops.agentinfra.dto.ChatSessionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatSessionService(ChatSessionRepository chatSessionRepository,
                              ChatMessageRepository chatMessageRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getUserSessions(UUID userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream().map(ChatSessionResponse::from).toList();
    }

    @Transactional
    public ChatSessionEntity createSession(UUID userId, UUID agentInstanceId) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setUserId(userId);
        session.setAgentInstanceId(agentInstanceId);
        return chatSessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        ChatSessionEntity session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session nicht gefunden"));
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Zugriff verweigert");
        }
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(UUID sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional
    public ChatMessageEntity saveMessage(UUID sessionId, String role, String content) {
        ChatMessageEntity msg = new ChatMessageEntity();
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
