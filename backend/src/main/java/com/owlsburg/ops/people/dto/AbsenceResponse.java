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
        String employeeFirstName,
        String employeeLastName,
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
                e.getId(), e.getEmployeeId(), null, null, e.getType(),
                e.getFromDate(), e.getToDate(), e.getStatus(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    public static AbsenceResponse from(AbsenceEntity e, String firstName, String lastName) {
        return new AbsenceResponse(
                e.getId(), e.getEmployeeId(), firstName, lastName, e.getType(),
                e.getFromDate(), e.getToDate(), e.getStatus(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
