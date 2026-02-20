package com.sindoflow.ops.people;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "absences")
@Getter
@Setter
@NoArgsConstructor
public class AbsenceEntity extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AbsenceType type;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AbsenceStatus status = AbsenceStatus.PENDING;

    @Column(name = "notes")
    private String notes;
}
