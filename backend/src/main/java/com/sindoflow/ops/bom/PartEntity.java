package com.sindoflow.ops.bom;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "parts")
@Getter
@Setter
@NoArgsConstructor
public class PartEntity extends BaseEntity {

    @Column(name = "part_number", nullable = false, unique = true, length = 50)
    private String partNumber;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PartType type;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
