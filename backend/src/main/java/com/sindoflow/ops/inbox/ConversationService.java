package com.sindoflow.ops.inbox;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final ConversationTagRepository tagRepository;
    private final ConversationLinkRepository linkRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationTagRepository tagRepository,
                               ConversationLinkRepository linkRepository) {
        this.conversationRepository = conversationRepository;
        this.tagRepository = tagRepository;
        this.linkRepository = linkRepository;
    }

    @Transactional(readOnly = true)
    public Page<ConversationEntity> findAll(Pageable pageable, String status) {
        if (status != null) {
            return conversationRepository.findByStatus(ConversationStatus.valueOf(status), pageable);
        }
        return conversationRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public ConversationEntity findById(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found: " + id));
    }

    @Transactional
    public ConversationEntity create(String subject, UUID customerId, ConversationPriority priority,
                                     ConversationSource source) {
        ConversationEntity conversation = new ConversationEntity();
        conversation.setSubject(subject);
        conversation.setCustomerId(customerId);
        conversation.setPriority(priority != null ? priority : ConversationPriority.NORMAL);
        conversation.setSource(source != null ? source : ConversationSource.MANUAL);
        conversation.setStatus(ConversationStatus.OPEN);

        log.info("Creating conversation: {}", subject);
        return conversationRepository.save(conversation);
    }

    @Transactional
    public ConversationEntity update(UUID id, String subject, UUID customerId, ConversationPriority priority) {
        ConversationEntity conversation = findById(id);
        if (subject != null) conversation.setSubject(subject);
        if (customerId != null) conversation.setCustomerId(customerId);
        if (priority != null) conversation.setPriority(priority);
        return conversationRepository.save(conversation);
    }

    @Transactional
    public ConversationEntity updateStatus(UUID id, ConversationStatus status) {
        ConversationEntity conversation = findById(id);
        conversation.setStatus(status);
        log.info("Updated conversation {} status to {}", id, status);
        return conversationRepository.save(conversation);
    }

    @Transactional
    public ConversationEntity assign(UUID id, UUID assignedTo) {
        ConversationEntity conversation = findById(id);
        conversation.setAssignedTo(assignedTo);
        if (conversation.getStatus() == ConversationStatus.OPEN) {
            conversation.setStatus(ConversationStatus.IN_PROGRESS);
        }
        log.info("Assigned conversation {} to {}", id, assignedTo);
        return conversationRepository.save(conversation);
    }

    @Transactional
    public ConversationTagEntity addTag(UUID conversationId, String tag) {
        findById(conversationId); // verify exists

        ConversationTagEntity tagEntity = new ConversationTagEntity();
        tagEntity.setConversationId(conversationId);
        tagEntity.setTag(tag);

        log.info("Adding tag '{}' to conversation {}", tag, conversationId);
        return tagRepository.save(tagEntity);
    }

    @Transactional
    public void removeTag(UUID conversationId, String tag) {
        findById(conversationId);
        tagRepository.deleteByConversationIdAndTag(conversationId, tag);
        log.info("Removed tag '{}' from conversation {}", tag, conversationId);
    }

    @Transactional
    public ConversationLinkEntity linkEntity(UUID conversationId, String linkedType, UUID linkedId) {
        findById(conversationId);

        ConversationLinkEntity link = new ConversationLinkEntity();
        link.setConversationId(conversationId);
        link.setLinkedType(linkedType);
        link.setLinkedId(linkedId);

        log.info("Linking conversation {} to {} {}", conversationId, linkedType, linkedId);
        return linkRepository.save(link);
    }
}
