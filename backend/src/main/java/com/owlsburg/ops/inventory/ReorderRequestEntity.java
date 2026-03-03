package com.owlsburg.ops.inventory;

import com.owlsburg.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reorder_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReorderRequestEntity extends BaseEntity {

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 30)
    private String status = "OPEN";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;
}
