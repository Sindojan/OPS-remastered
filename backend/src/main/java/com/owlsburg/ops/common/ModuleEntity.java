package com.owlsburg.ops.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
public class ModuleEntity {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_core", nullable = false)
    private boolean core;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
