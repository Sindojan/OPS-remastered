package com.sindoflow.ops.bom;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calculations")
@Getter
@Setter
@NoArgsConstructor
public class CalculationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @Column(name = "bom_version_id")
    private UUID bomVersionId;

    @Column(name = "process_plan_id")
    private UUID processPlanId;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "material_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal materialCost = BigDecimal.ZERO;

    @Column(name = "labor_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal laborCost = BigDecimal.ZERO;

    @Column(name = "overhead_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal overheadCost = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    @Column(name = "calculated_by")
    private UUID calculatedBy;
}
