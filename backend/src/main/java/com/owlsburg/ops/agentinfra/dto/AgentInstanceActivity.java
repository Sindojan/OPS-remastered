package com.owlsburg.ops.agentinfra.dto;

import java.time.Instant;
import java.util.UUID;

public record AgentInstanceActivity(
        UUID id,
        String name,
        String templateRole,
        String status,
        String activityStatus,
        UUID parentInstanceId,
        String model,
        String type,
        String currentTask,
        int tokensUsedToday,
        int dailyTokenBudget,
        UUID activeRunId,
        Instant lastActivityAt
) {}
