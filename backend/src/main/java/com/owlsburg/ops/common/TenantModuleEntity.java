package com.owlsburg.ops.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_modules")
@Getter
@Setter
@NoArgsConstructor
public class TenantModuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "module_id", nullable = false, length = 50)
    private String moduleId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "enabled_at", nullable = false)
    private Instant enabledAt;

    @PrePersist
    protected void prePersist() {
        if (tenantId == null) {
            String tid = TenantContext.getCurrentTenant();
            if (tid != null) {
                tenantId = UUID.fromString(tid);
            }
        }
        if (enabledAt == null) {
            enabledAt = Instant.now();
        }
    }
}
