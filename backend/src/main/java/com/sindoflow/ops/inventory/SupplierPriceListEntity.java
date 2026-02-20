package com.sindoflow.ops.inventory;

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
@Table(name = "supplier_price_lists")
@Getter
@Setter
@NoArgsConstructor
public class SupplierPriceListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "supplier_article_id", nullable = false)
    private UUID supplierArticleId;

    @Column(name = "price", nullable = false, precision = 12, scale = 4)
    private BigDecimal price;

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
