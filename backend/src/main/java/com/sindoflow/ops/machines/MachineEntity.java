package com.sindoflow.ops.machines;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "machines")
@Getter
@Setter
@NoArgsConstructor
public class MachineEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "machine_number", nullable = false, unique = true, length = 50)
    private String machineNumber;

    @Column(name = "type", length = 100)
    private String type;

    @Column(name = "station_id")
    private UUID stationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MachineStatus status = MachineStatus.AVAILABLE;

    @Column(name = "capacity_per_hour", precision = 8, scale = 2)
    private BigDecimal capacityPerHour;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;
}
