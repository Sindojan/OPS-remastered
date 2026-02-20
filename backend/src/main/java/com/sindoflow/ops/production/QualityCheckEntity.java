package com.sindoflow.ops.production;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quality_checks")
@Getter
@Setter
@NoArgsConstructor
public class QualityCheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "checked_by")
    private UUID checkedBy;

    @Column(name = "check_type", nullable = false, length = 100)
    private String checkType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private QualityResult result;

    @Column(name = "defect_count", nullable = false)
    private int defectCount;

    @Column(name = "notes")
    private String notes;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt = Instant.now();

    @OneToMany(mappedBy = "qualityCheckId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QualityDefectEntity> defects = new ArrayList<>();
}
