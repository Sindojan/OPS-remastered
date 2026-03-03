package com.owlsburg.ops.agentinfra.messaging;

import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentMessageBus {

    private static final Logger log = LoggerFactory.getLogger(AgentMessageBus.class);

    private final AgentMessageRepository messageRepository;
    private final AgentInstanceRepository instanceRepository;

    public AgentMessageBus(AgentMessageRepository messageRepository,
                           AgentInstanceRepository instanceRepository) {
        this.messageRepository = messageRepository;
        this.instanceRepository = instanceRepository;
    }

    @Transactional
    public AgentMessageEntity send(UUID senderInstanceId, UUID targetInstanceId,
                                    String messageType, String subject, String body,
                                    String priority) {
        // Validate hierarchy
        AgentLevel senderLevel = resolveLevel(senderInstanceId);
        AgentLevel targetLevel = resolveLevel(targetInstanceId);
        validateCommunication(senderInstanceId, targetInstanceId, senderLevel, targetLevel);

        AgentMessageEntity message = new AgentMessageEntity();
        message.setSenderInstanceId(senderInstanceId);
        message.setTargetInstanceId(targetInstanceId);
        message.setMessageType(messageType != null ? messageType : "INFO");
        message.setSubject(subject);
        message.setBody(body);
        message.setPriority(priority != null ? priority : "NORMAL");
        message.setStatus("PENDING");

        AgentMessageEntity saved = messageRepository.save(message);
        log.info("Agent message sent: {} → {} [{}] '{}'",
                senderInstanceId, targetInstanceId, messageType, subject);
        return saved;
    }

    @Transactional
    public List<AgentMessageEntity> getUnreadMessages(UUID instanceId) {
        List<AgentMessageEntity> messages = messageRepository
                .findByTargetInstanceIdAndStatusOrderByCreatedAtAsc(instanceId, "PENDING");

        for (AgentMessageEntity msg : messages) {
            msg.setStatus("DELIVERED");
            msg.setDeliveredAt(Instant.now());
        }

        if (!messages.isEmpty()) {
            messageRepository.saveAll(messages);
            log.debug("Delivered {} messages to instance {}", messages.size(), instanceId);
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

    public String buildMessageSection(UUID instanceId) {
        List<AgentMessageEntity> messages = messageRepository
                .findByTargetInstanceIdAndStatusOrderByCreatedAtAsc(instanceId, "PENDING");

        if (messages.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("\n\n## Nachrichten\n");
        for (AgentMessageEntity msg : messages) {
            sb.append("- **[").append(msg.getPriority()).append("]** ")
              .append(msg.getSubject()).append(": ").append(msg.getBody()).append("\n");
        }
        return sb.toString();
    }

    private AgentLevel resolveLevel(UUID instanceId) {
        AgentInstanceEntity instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Agent-Instanz nicht gefunden: " + instanceId));

        if (instance.getParentInstanceId() == null) {
            return AgentLevel.CEO;
        }

        // Check if parent has a parent (grandparent = sub-agent)
        AgentInstanceEntity parent = instanceRepository.findById(instance.getParentInstanceId())
                .orElse(null);
        if (parent != null && parent.getParentInstanceId() != null) {
            return AgentLevel.SUB_AGENT;
        }

        return AgentLevel.LEAD;
    }

    private void validateCommunication(UUID senderId, UUID targetId,
                                        AgentLevel senderLevel, AgentLevel targetLevel) {
        // CEO → Lead: always allowed
        // Lead → CEO: always allowed
        // Lead → Lead: allowed (peer communication)
        if (senderLevel == AgentLevel.CEO || targetLevel == AgentLevel.CEO) {
            return; // CEO can communicate with anyone
        }

        if (senderLevel == AgentLevel.LEAD && targetLevel == AgentLevel.LEAD) {
            return; // Peer communication
        }

        if (senderLevel == AgentLevel.LEAD && targetLevel == AgentLevel.SUB_AGENT) {
            // Lead → Sub: only own subs
            AgentInstanceEntity target = instanceRepository.findById(targetId).orElse(null);
            if (target != null && senderId.equals(target.getParentInstanceId())) {
                return;
            }
            throw new IllegalArgumentException("Lead kann nur eigene Sub-Agents adressieren");
        }

        if (senderLevel == AgentLevel.SUB_AGENT && targetLevel == AgentLevel.LEAD) {
            // Sub → Lead: only own parent
            AgentInstanceEntity sender = instanceRepository.findById(senderId).orElse(null);
            if (sender != null && targetId.equals(sender.getParentInstanceId())) {
                return;
            }
            throw new IllegalArgumentException("Sub-Agent kann nur den eigenen Lead adressieren");
        }

        if (senderLevel == AgentLevel.SUB_AGENT && targetLevel == AgentLevel.SUB_AGENT) {
            // Sub → Sub: only same parent
            AgentInstanceEntity sender = instanceRepository.findById(senderId).orElse(null);
            AgentInstanceEntity target = instanceRepository.findById(targetId).orElse(null);
            if (sender != null && target != null
                    && sender.getParentInstanceId() != null
                    && sender.getParentInstanceId().equals(target.getParentInstanceId())) {
                return;
            }
            throw new IllegalArgumentException("Sub-Agents können nur Geschwister-Agents adressieren");
        }

        throw new IllegalArgumentException("Kommunikation nicht erlaubt: "
                + senderLevel + " → " + targetLevel);
    }
}
