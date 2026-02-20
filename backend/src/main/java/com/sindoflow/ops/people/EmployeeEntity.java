package com.sindoflow.ops.people;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeEntity extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "employee_number", nullable = false, unique = true, length = 50)
    private String employeeNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "role", nullable = false, length = 50)
    private String role = "WORKER";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "station_id")
    private UUID stationId;
}
