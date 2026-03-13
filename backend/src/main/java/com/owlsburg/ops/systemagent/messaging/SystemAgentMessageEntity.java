package com.owlsburg.ops.systemagent.messaging;

import com.owlsburg.ops.systemagent.SystemBaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_agent_messages")
@Getter
@Setter
@NoArgsConstructor
public class SystemAgentMessageEntity extends SystemBaseEntity {

    @Column(name = "sender_instance_id", nullable = false)
    private UUID senderInstanceId;

    @Column(name = "target_instance_id", nullable = false)
    private UUID targetInstanceId;

    @Column(name = "message_type", nullable = false, length = 30)
    private String messageType = "INFO";

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 10)
    private String priority = "NORMAL";

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "parent_message_id")
    private UUID parentMessageId;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
