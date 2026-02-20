package com.sindoflow.ops.inbox;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;

    public MessageService(ConversationRepository conversationRepository,
                          ConversationMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public ConversationMessageEntity addMessage(UUID conversationId, String content,
                                                SenderType senderType, UUID senderId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found: " + conversationId));

        ConversationMessageEntity message = new ConversationMessageEntity();
        message.setConversation(conversation);
        message.setContent(content);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setSentAt(Instant.now());

        log.info("Adding message to conversation {} from {} {}", conversationId, senderType, senderId);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Page<ConversationMessageEntity> getMessages(UUID conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId, pageable);
    }
}
