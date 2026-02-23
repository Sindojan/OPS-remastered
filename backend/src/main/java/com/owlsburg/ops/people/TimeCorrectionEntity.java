package com.owlsburg.ops.people;

import com.owlsburg.ops.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "time_corrections")
@Getter
@Setter
@NoArgsConstructor
public class TimeCorrectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "time_entry_id", nullable = false)
    private UUID timeEntryId;

    @Column(name = "corrected_by", nullable = false)
    private UUID correctedBy;

    @Column(name = "old_timestamp", nullable = false)
    private Instant oldTimestamp;

    @Column(name = "new_timestamp", nullable = false)
    private Instant newTimestamp;

    @Column(name = "reason", nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

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
