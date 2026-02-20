package com.sindoflow.ops.production;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class JobEntity extends BaseEntity {

    @Column(name = "job_number", nullable = false, unique = true, length = 50)
    private String jobNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "deadline")
    private Instant deadline;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "assigned_station_id")
    private UUID assignedStationId;

    @Column(name = "shift_id")
    private UUID shiftId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
