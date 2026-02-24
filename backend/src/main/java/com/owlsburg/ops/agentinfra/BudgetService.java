package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.BudgetOverviewResponse;
import com.owlsburg.ops.agentinfra.dto.BudgetOverviewResponse.AgentBudget;
import com.owlsburg.ops.agentinfra.dto.BudgetOverviewResponse.BudgetPeriod;
import com.owlsburg.ops.agentinfra.dto.BudgetOverviewResponse.DailyUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    private final AgentRunRepository runRepository;
    private final AgentInstanceRepository instanceRepository;

    public BudgetService(AgentRunRepository runRepository,
                         AgentInstanceRepository instanceRepository) {
        this.runRepository = runRepository;
        this.instanceRepository = instanceRepository;
    }

    @Transactional(readOnly = true)
    public BudgetOverviewResponse getOverview() {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);

        // Current month range
        LocalDate monthStart = now.withDayOfMonth(1);
        Instant monthStartInstant = monthStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant nowInstant = now.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Last 30 days range
        LocalDate thirtyDaysAgo = now.minusDays(30);
        Instant thirtyDaysAgoInstant = thirtyDaysAgo.atStartOfDay(ZoneOffset.UTC).toInstant();

        // Load instance name lookup
        Map<UUID, String> instanceNames = new HashMap<>();
        instanceRepository.findAll().forEach(inst -> instanceNames.put(inst.getId(), inst.getName()));

        // Fetch runs for both periods (last 30 days covers current month too)
        List<AgentRunEntity> last30DaysRuns = runRepository.findByStartedAtBetween(thirtyDaysAgoInstant, nowInstant);

        // Filter current month runs
        List<AgentRunEntity> currentMonthRuns = last30DaysRuns.stream()
                .filter(r -> r.getStartedAt() != null && !r.getStartedAt().isBefore(monthStartInstant))
                .toList();

        BudgetPeriod currentMonth = buildPeriod(currentMonthRuns, instanceNames);
        BudgetPeriod last30Days = buildPeriod(last30DaysRuns, instanceNames);

        // Daily usage for last 30 days
        List<DailyUsage> dailyUsage = buildDailyUsage(last30DaysRuns);

        return new BudgetOverviewResponse(currentMonth, last30Days, dailyUsage);
    }

    private BudgetPeriod buildPeriod(List<AgentRunEntity> runs, Map<UUID, String> instanceNames) {
        BigDecimal totalCost = BigDecimal.ZERO;
        long totalTokens = 0;

        Map<UUID, BigDecimal> costByInstance = new HashMap<>();
        Map<UUID, Long> tokensByInstance = new HashMap<>();
        Map<UUID, Integer> runsByInstance = new HashMap<>();

        for (AgentRunEntity run : runs) {
            totalCost = totalCost.add(run.getCostUsd() != null ? run.getCostUsd() : BigDecimal.ZERO);
            totalTokens += run.getTokensUsed();

            UUID instId = run.getInstanceId();
            costByInstance.merge(instId, run.getCostUsd() != null ? run.getCostUsd() : BigDecimal.ZERO, BigDecimal::add);
            tokensByInstance.merge(instId, (long) run.getTokensUsed(), Long::sum);
            runsByInstance.merge(instId, 1, Integer::sum);
        }

        List<AgentBudget> byAgent = costByInstance.keySet().stream()
                .map(instId -> new AgentBudget(
                        instanceNames.getOrDefault(instId, "Unknown Agent"),
                        costByInstance.get(instId),
                        tokensByInstance.getOrDefault(instId, 0L),
                        runsByInstance.getOrDefault(instId, 0)
                ))
                .sorted(Comparator.comparing(AgentBudget::costUsd).reversed())
                .toList();

        return new BudgetPeriod(totalCost, totalTokens, runs.size(), byAgent);
    }

    private List<DailyUsage> buildDailyUsage(List<AgentRunEntity> runs) {
        Map<LocalDate, BigDecimal> costByDate = new TreeMap<>();
        Map<LocalDate, Long> tokensByDate = new TreeMap<>();

        for (AgentRunEntity run : runs) {
            if (run.getStartedAt() == null) continue;
            LocalDate date = run.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate();
            costByDate.merge(date, run.getCostUsd() != null ? run.getCostUsd() : BigDecimal.ZERO, BigDecimal::add);
            tokensByDate.merge(date, (long) run.getTokensUsed(), Long::sum);
        }

        return costByDate.entrySet().stream()
                .map(entry -> new DailyUsage(
                        entry.getKey(),
                        entry.getValue(),
                        tokensByDate.getOrDefault(entry.getKey(), 0L)
                ))
                .toList();
    }
}
