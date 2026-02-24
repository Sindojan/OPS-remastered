package com.owlsburg.ops.tenant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 50)
    private String plan = "BASIC";

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspend_reason")
    private String suspendReason;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
