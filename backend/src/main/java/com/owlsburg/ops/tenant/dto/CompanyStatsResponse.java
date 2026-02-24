package com.owlsburg.ops.tenant.dto;

import java.time.Instant;

public record CompanyStatsResponse(
        long userCount,
        long activeAgentRuns30d,
        long storageUsedMb,
        Instant lastActiveAt
) {}
