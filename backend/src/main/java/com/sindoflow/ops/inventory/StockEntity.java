package com.sindoflow.ops.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
public class StockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "warehouse_location_id", nullable = false)
    private UUID warehouseLocationId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
