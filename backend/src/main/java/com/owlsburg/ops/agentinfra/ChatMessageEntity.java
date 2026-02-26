package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersistTenant() {
        if (tenantId == null) {
            String tid = TenantContext.getCurrentTenant();
            if (tid != null) {
                tenantId = UUID.fromString(tid);
            }
        }
    }
}
