package com.owlsburg.ops.bom;

import com.owlsburg.ops.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bom_items")
@Getter
@Setter
@NoArgsConstructor
public class BomItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "bom_version_id", nullable = false)
    private UUID bomVersionId;

    @Column(name = "component_part_id", nullable = false)
    private UUID componentPartId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "position", nullable = false)
    private int position;

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
