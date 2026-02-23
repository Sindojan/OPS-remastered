package com.owlsburg.ops.production.dto;

import com.owlsburg.ops.common.SeverityLevel;

import java.util.UUID;

public record QualityDefectResponse(
        UUID id,
        String defectType,
        String description,
        SeverityLevel severity
) {}
