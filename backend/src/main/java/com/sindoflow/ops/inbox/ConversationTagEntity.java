package com.sindoflow.ops.inbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "conversation_tags")
@IdClass(ConversationTagEntity.ConversationTagId.class)
@Getter
@Setter
@NoArgsConstructor
public class ConversationTagEntity {

    @Id
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Id
    @Column(nullable = false, length = 100)
    private String tag;

    public static class ConversationTagId implements Serializable {
        private UUID conversationId;
        private String tag;

        public ConversationTagId() {}

        public ConversationTagId(UUID conversationId, String tag) {
            this.conversationId = conversationId;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConversationTagId that = (ConversationTagId) o;
            return Objects.equals(conversationId, that.conversationId) && Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conversationId, tag);
        }
    }
}
