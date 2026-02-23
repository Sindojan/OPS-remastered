package com.owlsburg.ops.production;

import com.owlsburg.ops.common.SeverityLevel;
import com.owlsburg.ops.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "quality_defects")
@Getter
@Setter
@NoArgsConstructor
public class QualityDefectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "quality_check_id", nullable = false)
    private UUID qualityCheckId;

    @Column(name = "defect_type", nullable = false, length = 100)
    private String defectType;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private SeverityLevel severity = SeverityLevel.MEDIUM;

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
