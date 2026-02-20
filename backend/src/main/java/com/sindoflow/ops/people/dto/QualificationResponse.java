package com.sindoflow.ops.people.dto;

import com.sindoflow.ops.people.EmployeeQualificationEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record QualificationResponse(
        UUID id,
        UUID employeeId,
        String qualification,
        LocalDate certifiedAt,
        LocalDate expiresAt,
        Instant createdAt
) {
    public static QualificationResponse from(EmployeeQualificationEntity e) {
        return new QualificationResponse(
                e.getId(), e.getEmployeeId(), e.getQualification(),
                e.getCertifiedAt(), e.getExpiresAt(), e.getCreatedAt()
        );
    }
}
