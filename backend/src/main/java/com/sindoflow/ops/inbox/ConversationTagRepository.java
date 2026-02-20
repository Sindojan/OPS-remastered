package com.sindoflow.ops.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationTagRepository extends JpaRepository<ConversationTagEntity, ConversationTagEntity.ConversationTagId> {

    List<ConversationTagEntity> findByConversationId(UUID conversationId);

    void deleteByConversationIdAndTag(UUID conversationId, String tag);
}
