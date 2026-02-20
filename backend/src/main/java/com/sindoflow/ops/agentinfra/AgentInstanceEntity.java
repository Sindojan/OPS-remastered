package com.sindoflow.ops.agentinfra;

import com.sindoflow.ops.common.BaseEntity;
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

    @Column(name = "tenant_id", length = 63)
    private String tenantId;

    @Column(columnDefinition = "jsonb")
    private String config = "{}";

    @Column(name = "terminated_at")
    private Instant terminatedAt;
}
