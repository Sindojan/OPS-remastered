package com.sindoflow.ops.production.dto;

import com.sindoflow.ops.common.SeverityLevel;

import java.util.UUID;

public record QualityDefectResponse(
        UUID id,
        String defectType,
        String description,
        SeverityLevel severity
) {}
