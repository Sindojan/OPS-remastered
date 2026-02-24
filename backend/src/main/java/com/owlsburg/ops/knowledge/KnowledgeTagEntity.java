package com.owlsburg.ops.knowledge;

import com.owlsburg.ops.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "knowledge_tags")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

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
