package com.sindoflow.ops.inventory;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
public class ArticleEntity extends BaseEntity {

    @Column(name = "article_number", nullable = false, unique = true, length = 50)
    private String articleNumber;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "min_stock", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Column(name = "reorder_point", nullable = false, precision = 12, scale = 3)
    private BigDecimal reorderPoint = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
