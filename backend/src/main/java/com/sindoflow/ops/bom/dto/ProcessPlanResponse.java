package com.sindoflow.ops.bom.dto;

import com.sindoflow.ops.bom.ProcessPlanEntity;
import com.sindoflow.ops.bom.VersionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProcessPlanResponse(
        UUID id,
        UUID partId,
        int versionNumber,
        String name,
        VersionStatus status,
        LocalDate validFrom,
        UUID createdBy,
        Instant createdAt
) {
    public static ProcessPlanResponse from(ProcessPlanEntity e) {
        return new ProcessPlanResponse(
                e.getId(), e.getPartId(), e.getVersionNumber(),
                e.getName(), e.getStatus(), e.getValidFrom(),
                e.getCreatedBy(), e.getCreatedAt()
        );
    }
}
