package com.owlsburg.ops.bom;

import com.owlsburg.ops.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_calculations")
@Getter
@Setter
@NoArgsConstructor
public class JobCalculationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "calculation_id", nullable = false)
    private UUID calculationId;

    @Column(name = "actual_material_cost", precision = 12, scale = 4)
    private BigDecimal actualMaterialCost;

    @Column(name = "actual_labor_cost", precision = 12, scale = 4)
    private BigDecimal actualLaborCost;

    @Column(name = "actual_total_cost", precision = 12, scale = 4)
    private BigDecimal actualTotalCost;

    @Column(name = "variance_percent", precision = 8, scale = 2)
    private BigDecimal variancePercent;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

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
