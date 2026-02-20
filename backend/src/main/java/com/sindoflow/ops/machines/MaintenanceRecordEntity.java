package com.sindoflow.ops.machines;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_records")
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceRecordEntity extends BaseEntity {

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "interval_id")
    private UUID intervalId;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "performed_at")
    private Instant performedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MaintenanceRecordStatus status = MaintenanceRecordStatus.PLANNED;
}
