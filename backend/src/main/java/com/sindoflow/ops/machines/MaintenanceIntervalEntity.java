package com.sindoflow.ops.machines;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_intervals")
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceIntervalEntity extends BaseEntity {

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MaintenanceType type;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "interval_hours")
    private Integer intervalHours;

    @Column(name = "last_performed_at")
    private Instant lastPerformedAt;

    @Column(name = "next_due_at")
    private Instant nextDueAt;

    @Column(name = "description")
    private String description;
}
