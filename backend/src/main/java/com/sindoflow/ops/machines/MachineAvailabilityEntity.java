package com.sindoflow.ops.machines;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "machine_availability")
@Getter
@Setter
@NoArgsConstructor
public class MachineAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "shift_id", nullable = false)
    private UUID shiftId;

    @Column(name = "available_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal availableHours;
}
