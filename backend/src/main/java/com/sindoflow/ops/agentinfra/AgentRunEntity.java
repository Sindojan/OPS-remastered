package com.sindoflow.ops.agentinfra;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_runs")
@Getter
@Setter
@NoArgsConstructor
public class AgentRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    @Column(name = "trigger_source")
    private String triggerSource;

    @Column(name = "input_context", columnDefinition = "jsonb")
    private String inputContext;

    @Column(columnDefinition = "jsonb")
    private String output;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentRunStatus status = AgentRunStatus.PENDING;

    @Column(name = "tokens_used", nullable = false)
    private int tokensUsed = 0;

    @Column(name = "cost_usd", nullable = false, precision = 10, scale = 6)
    private BigDecimal costUsd = BigDecimal.ZERO;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
