package com.sindoflow.ops.production.dto;

import com.sindoflow.ops.production.QualityResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QualityCheckResponse(
        UUID id,
        UUID jobId,
        UUID checkedBy,
        String checkType,
        QualityResult result,
        int defectCount,
        String notes,
        Instant checkedAt,
        List<QualityDefectResponse> defects
) {}
