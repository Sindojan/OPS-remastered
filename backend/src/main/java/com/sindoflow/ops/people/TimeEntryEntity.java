package com.sindoflow.ops.people;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "time_entries")
@Getter
@Setter
@NoArgsConstructor
public class TimeEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TimeEntryType type;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
