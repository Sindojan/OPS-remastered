package com.sindoflow.ops.production;

import com.sindoflow.ops.common.SeverityLevel;
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

    @Column(name = "quality_check_id", nullable = false)
    private UUID qualityCheckId;

    @Column(name = "defect_type", nullable = false, length = 100)
    private String defectType;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private SeverityLevel severity = SeverityLevel.MEDIUM;
}
