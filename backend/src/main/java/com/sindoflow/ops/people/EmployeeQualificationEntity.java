package com.sindoflow.ops.people;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employee_qualifications")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeQualificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "qualification", nullable = false, length = 255)
    private String qualification;

    @Column(name = "certified_at")
    private LocalDate certifiedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
