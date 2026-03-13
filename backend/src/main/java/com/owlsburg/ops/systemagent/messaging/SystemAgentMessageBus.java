package com.owlsburg.ops.systemagent.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SystemAgentMessageBus {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentMessageBus.class);

    private final SystemAgentMessageRepository messageRepository;

    public SystemAgentMessageBus(SystemAgentMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public SystemAgentMessageEntity send(UUID senderInstanceId, UUID targetInstanceId,
                                          String messageType, String subject, String body, String priority) {
        SystemAgentMessageEntity msg = new SystemAgentMessageEntity();
        msg.setSenderInstanceId(senderInstanceId);
        msg.setTargetInstanceId(targetInstanceId);
        msg.setMessageType(messageType);
        msg.setSubject(subject);
        msg.setBody(body);
        msg.setPriority(priority != null ? priority : "NORMAL");
        msg.setStatus("PENDING");

        log.debug("System agent message: {} -> {} [{}]", senderInstanceId, targetInstanceId, subject);
        return messageRepository.save(msg);
    }

    @Transactional
    public List<SystemAgentMessageEntity> getUnreadMessages(UUID instanceId) {
        List<SystemAgentMessageEntity> messages =
                messageRepository.findByTargetInstanceIdAndStatusOrderByCreatedAtAsc(instanceId, "PENDING");
        for (SystemAgentMessageEntity msg : messages) {
            msg.setStatus("DELIVERED");
            msg.setDeliveredAt(Instant.now());
            messageRepository.save(msg);
        }
        return messages;
    }

    @Transactional
    public void markAsRead(UUID messageId) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            msg.setStatus("READ");
            msg.setReadAt(Instant.now());
            messageRepository.save(msg);
        });
    }

    @Transactional(readOnly = true)
    public String buildMessageSection(UUID instanceId) {
        List<SystemAgentMessageEntity> pending =
                messageRepository.findByTargetInstanceIdAndStatusOrderByCreatedAtAsc(instanceId, "PENDING");
        if (pending.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("\n\n## Ungelesene Nachrichten\n");
        for (SystemAgentMessageEntity msg : pending) {
            sb.append("- Von Agent ").append(msg.getSenderInstanceId())
                    .append(": [").append(msg.getSubject()).append("] ").append(msg.getBody()).append("\n");
        }
        return sb.toString();
    }
}
