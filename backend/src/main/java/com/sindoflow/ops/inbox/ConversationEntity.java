package com.sindoflow.ops.inbox;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
public class ConversationEntity extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationStatus status = ConversationStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationPriority priority = ConversationPriority.NORMAL;

    @Column(name = "sla_due_at")
    private Instant slaDueAt;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationSource source = ConversationSource.MANUAL;
}
