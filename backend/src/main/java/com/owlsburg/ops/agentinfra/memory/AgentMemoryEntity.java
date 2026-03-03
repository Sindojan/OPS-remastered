package com.owlsburg.ops.agentinfra.memory;

import com.owlsburg.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_memories")
@Getter
@Setter
@NoArgsConstructor
public class AgentMemoryEntity extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(nullable = false, length = 30)
    private String type = "NOTE";

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 255)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false)
    private int importance = 5;

    @Column(name = "last_accessed_at", nullable = false)
    private Instant lastAccessedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;
}
