package com.sindoflow.ops.bom;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cost_rates")
@Getter
@Setter
@NoArgsConstructor
public class CostRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "rate_per_hour", nullable = false, precision = 10, scale = 4)
    private BigDecimal ratePerHour;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
