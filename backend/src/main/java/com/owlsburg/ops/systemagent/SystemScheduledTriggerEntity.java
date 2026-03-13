package com.owlsburg.ops.systemagent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_scheduled_triggers")
@Getter
@Setter
@NoArgsConstructor
public class SystemScheduledTriggerEntity extends SystemBaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(name = "task_description", nullable = false, columnDefinition = "TEXT")
    private String taskDescription;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_run_at")
    private Instant lastRunAt;
}
