package com.sindoflow.ops.inbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessageEntity, UUID> {

    Page<ConversationMessageEntity> findByConversationIdOrderBySentAtAsc(UUID conversationId, Pageable pageable);
}
