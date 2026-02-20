package com.sindoflow.ops.production;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stations")
@Getter
@Setter
@NoArgsConstructor
public class StationEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "capacity_per_shift")
    private Integer capacityPerShift;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StationShiftEntity> stationShifts = new ArrayList<>();
}
