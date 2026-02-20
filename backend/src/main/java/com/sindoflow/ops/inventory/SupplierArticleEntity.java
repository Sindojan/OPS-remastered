package com.sindoflow.ops.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "supplier_articles")
@Getter
@Setter
@NoArgsConstructor
public class SupplierArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "supplier_article_number", length = 100)
    private String supplierArticleNumber;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "min_order_quantity", precision = 12, scale = 3)
    private BigDecimal minOrderQuantity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
