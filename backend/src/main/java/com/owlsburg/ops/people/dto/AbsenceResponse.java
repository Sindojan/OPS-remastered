package com.owlsburg.ops.people.dto;

import com.owlsburg.ops.people.AbsenceEntity;
import com.owlsburg.ops.people.AbsenceStatus;
import com.owlsburg.ops.people.AbsenceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AbsenceResponse(
        UUID id,
        UUID employeeId,
        AbsenceType type,
        LocalDate fromDate,
        LocalDate toDate,
        AbsenceStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static AbsenceResponse from(AbsenceEntity e) {
        return new AbsenceResponse(
                e.getId(), e.getEmployeeId(), e.getType(),
                e.getFromDate(), e.getToDate(), e.getStatus(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
