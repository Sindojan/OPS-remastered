package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_instances")
@Getter
@Setter
@NoArgsConstructor
public class AgentInstanceEntity extends BaseEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_instance_id")
    private UUID parentInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentInstanceType type = AgentInstanceType.PERSISTENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentInstanceStatus status = AgentInstanceStatus.INACTIVE;

    @Column(columnDefinition = "jsonb")
    private String config = "{}";

    @Column(name = "custom_system_prompt", columnDefinition = "TEXT")
    private String customSystemPrompt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_status", nullable = false)
    private AgentActivityStatus activityStatus = AgentActivityStatus.IDLE;

    @Column(name = "last_run_id")
    private UUID lastRunId;

    @Column(name = "activity_status_changed_at")
    private Instant activityStatusChangedAt;

    @Column(name = "allowed_tools_override", columnDefinition = "jsonb")
    private String allowedToolsOverride;
}
