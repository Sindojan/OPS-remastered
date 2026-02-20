package com.sindoflow.ops.events;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_triggers")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledTriggerEntity extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(nullable = false)
    private boolean active = true;
}
