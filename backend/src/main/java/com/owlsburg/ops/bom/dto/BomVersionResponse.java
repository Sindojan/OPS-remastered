package com.owlsburg.ops.bom.dto;

import com.owlsburg.ops.bom.BomVersionEntity;
import com.owlsburg.ops.bom.VersionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BomVersionResponse(
        UUID id,
        UUID partId,
        int versionNumber,
        VersionStatus status,
        LocalDate validFrom,
        UUID createdBy,
        Instant createdAt
) {
    public static BomVersionResponse from(BomVersionEntity e) {
        return new BomVersionResponse(
                e.getId(), e.getPartId(), e.getVersionNumber(),
                e.getStatus(), e.getValidFrom(), e.getCreatedBy(), e.getCreatedAt()
        );
    }
}
