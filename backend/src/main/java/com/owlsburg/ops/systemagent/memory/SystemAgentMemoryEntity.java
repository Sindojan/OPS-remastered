package com.owlsburg.ops.systemagent.memory;

import com.owlsburg.ops.systemagent.SystemBaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_agent_memories")
@Getter
@Setter
@NoArgsConstructor
public class SystemAgentMemoryEntity extends SystemBaseEntity {

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

    @Column(length = 50)
    private String source = "agent";

    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "run_id")
    private UUID runId;

    @Column(precision = 3, scale = 2)
    private BigDecimal confidence = BigDecimal.ONE;
}
