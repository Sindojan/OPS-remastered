package com.sindoflow.ops.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationLinkRepository extends JpaRepository<ConversationLinkEntity, UUID> {

    List<ConversationLinkEntity> findByConversationId(UUID conversationId);
}
