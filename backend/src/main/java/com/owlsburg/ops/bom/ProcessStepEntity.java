package com.owlsburg.ops.bom;

import com.owlsburg.ops.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "process_steps")
@Getter
@Setter
@NoArgsConstructor
public class ProcessStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "process_plan_id", nullable = false)
    private UUID processPlanId;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "station_id")
    private UUID stationId;

    @Column(name = "machine_id")
    private UUID machineId;

    @Column(name = "setup_time_minutes", nullable = false)
    private int setupTimeMinutes;

    @Column(name = "processing_time_minutes", nullable = false)
    private int processingTimeMinutes;

    @Column(name = "notes")
    private String notes;

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
