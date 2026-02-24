package com.owlsburg.ops.agentinfra.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BudgetOverviewResponse(
        BudgetPeriod currentMonth,
        BudgetPeriod last30Days,
        List<DailyUsage> dailyUsage
) {

    public record BudgetPeriod(
            BigDecimal totalCostUsd,
            long totalTokens,
            int totalRuns,
            List<AgentBudget> byAgent
    ) {}

    public record AgentBudget(
            String agentName,
            BigDecimal costUsd,
            long tokens,
            int runs
    ) {}

    public record DailyUsage(
            LocalDate date,
            BigDecimal costUsd,
            long tokens
    ) {}
}
