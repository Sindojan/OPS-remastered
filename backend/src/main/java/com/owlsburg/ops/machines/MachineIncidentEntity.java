package com.owlsburg.ops.machines;

import com.owlsburg.ops.common.SeverityLevel;
import com.owlsburg.ops.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_incidents")
@Getter
@Setter
@NoArgsConstructor
public class MachineIncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "reported_by")
    private UUID reportedBy;

    @Column(name = "type", nullable = false, length = 100)
    private String type;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private SeverityLevel severity = SeverityLevel.MEDIUM;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @PrePersist
    protected void prePersistTenant() {
        if (tenantId == null) {
            String tid = TenantContext.getCurrentTenant();
            if (tid != null) {
                tenantId = UUID.fromString(tid);
            }
        }
    }
}
