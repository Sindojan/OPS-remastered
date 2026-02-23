package com.owlsburg.ops.agentinfra.dto;

import com.owlsburg.ops.agentinfra.AgentRunService;

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
