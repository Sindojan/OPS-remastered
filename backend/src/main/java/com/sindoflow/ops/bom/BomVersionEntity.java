package com.sindoflow.ops.bom;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bom_versions")
@Getter
@Setter
@NoArgsConstructor
public class BomVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VersionStatus status = VersionStatus.DRAFT;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
