package com.sindoflow.ops.agentinfra;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_run_steps")
@Getter
@Setter
@NoArgsConstructor
public class AgentRunStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentStepType type;

    @Column(name = "tool_name")
    private String toolName;

    @Column(columnDefinition = "jsonb")
    private String input;

    @Column(columnDefinition = "jsonb")
    private String output;

    @Column(name = "tokens_used", nullable = false)
    private int tokensUsed = 0;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
