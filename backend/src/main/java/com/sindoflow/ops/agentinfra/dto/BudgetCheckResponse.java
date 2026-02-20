package com.sindoflow.ops.agentinfra.dto;

import com.sindoflow.ops.agentinfra.AgentRunService;

import java.util.UUID;

public record BudgetCheckResponse(
        UUID instanceId,
        int dailyBudget,
        int tokensUsedToday,
        int tokensRemaining
) {
    public static BudgetCheckResponse from(AgentRunService.BudgetCheckResult result) {
        return new BudgetCheckResponse(
                result.instanceId(),
                result.dailyBudget(),
                result.tokensUsedToday(),
                result.tokensRemaining()
        );
    }
}
