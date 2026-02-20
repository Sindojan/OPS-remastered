package com.sindoflow.ops.inbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_links")
@Getter
@Setter
@NoArgsConstructor
public class ConversationLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "linked_type", nullable = false, length = 50)
    private String linkedType;

    @Column(name = "linked_id", nullable = false)
    private UUID linkedId;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
