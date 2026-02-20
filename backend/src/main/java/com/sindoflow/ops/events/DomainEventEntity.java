package com.sindoflow.ops.events;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "domain_events")
@Getter
@Setter
@NoArgsConstructor
public class DomainEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "source_type", nullable = false, length = 100)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload = "{}";

    @Column(nullable = false)
    private boolean processed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
