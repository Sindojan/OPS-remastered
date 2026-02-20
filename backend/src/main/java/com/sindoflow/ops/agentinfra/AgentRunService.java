package com.sindoflow.ops.agentinfra;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class AgentRunService {

    private static final Logger log = LoggerFactory.getLogger(AgentRunService.class);

    private final AgentRunRepository runRepository;
    private final AgentRunStepRepository stepRepository;
    private final AgentInstanceRepository instanceRepository;
    private final AgentTemplateRepository templateRepository;

    public AgentRunService(AgentRunRepository runRepository,
                           AgentRunStepRepository stepRepository,
                           AgentInstanceRepository instanceRepository,
                           AgentTemplateRepository templateRepository) {
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.instanceRepository = instanceRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public AgentRunEntity findById(UUID id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AgentRun not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AgentRunEntity> findByInstanceId(UUID instanceId) {
        return runRepository.findByInstanceId(instanceId);
    }

    @Transactional(readOnly = true)
    public List<AgentRunEntity> findByStatus(AgentRunStatus status) {
        return runRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<AgentRunEntity> findAll() {
        return runRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AgentRunStepEntity> findStepsByRunId(UUID runId) {
        return stepRepository.findByRunIdOrderByStepNumber(runId);
    }

    @Transactional
    public AgentRunEntity startRun(UUID instanceId, TriggerType triggerType,
                                   String triggerSource, String inputContext) {
        // Verify instance exists
        instanceRepository.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("AgentInstance not found: " + instanceId));

        AgentRunEntity run = new AgentRunEntity();
        run.setInstanceId(instanceId);
        run.setTriggerType(triggerType);
        run.setTriggerSource(triggerSource);
        run.setInputContext(inputContext);
        run.setStatus(AgentRunStatus.PENDING);
        run.setStartedAt(Instant.now());

        log.info("Starting agent run for instance {} (trigger={})", instanceId, triggerType);
        return runRepository.save(run);
    }

    @Transactional
    public AgentRunStepEntity addStep(UUID runId, AgentStepType type, String toolName,
                                      String input, String output, int tokensUsed, int durationMs) {
        // Verify run exists
        findById(runId);

        // Determine next step number
        List<AgentRunStepEntity> existingSteps = stepRepository.findByRunIdOrderByStepNumber(runId);
        int nextStepNumber = existingSteps.isEmpty() ? 1 : existingSteps.get(existingSteps.size() - 1).getStepNumber() + 1;

        AgentRunStepEntity step = new AgentRunStepEntity();
        step.setRunId(runId);
        step.setStepNumber(nextStepNumber);
        step.setType(type);
        step.setToolName(toolName);
        step.setInput(input);
        step.setOutput(output);
        step.setTokensUsed(tokensUsed);
        step.setDurationMs(durationMs);

        log.info("Adding step {} to run {} (type={}, tool={})", nextStepNumber, runId, type, toolName);
        return stepRepository.save(step);
    }

    @Transactional
    public AgentRunEntity completeRun(UUID runId, String output, int totalTokensUsed, BigDecimal costUsd) {
        AgentRunEntity run = findById(runId);
        run.setStatus(AgentRunStatus.SUCCESS);
        run.setOutput(output);
        run.setTokensUsed(totalTokensUsed);
        run.setCostUsd(costUsd);
        run.setCompletedAt(Instant.now());

        log.info("Completed agent run {} (tokens={}, cost={})", runId, totalTokensUsed, costUsd);
        return runRepository.save(run);
    }

    @Transactional
    public AgentRunEntity failRun(UUID runId, String errorMessage) {
        AgentRunEntity run = findById(runId);
        run.setStatus(AgentRunStatus.FAILED);
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(Instant.now());

        log.info("Failed agent run {}: {}", runId, errorMessage);
        return runRepository.save(run);
    }

    @Transactional
    public AgentRunEntity cancelRun(UUID runId) {
        AgentRunEntity run = findById(runId);
        run.setStatus(AgentRunStatus.CANCELLED);
        run.setCompletedAt(Instant.now());

        log.info("Cancelled agent run {}", runId);
        return runRepository.save(run);
    }

    /**
     * Checks the token budget for an agent instance.
     * Returns the remaining tokens for today based on the template's dailyTokenBudget.
     */
    @Transactional(readOnly = true)
    public BudgetCheckResult checkBudget(UUID instanceId) {
        AgentInstanceEntity instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("AgentInstance not found: " + instanceId));

        AgentTemplateEntity template = templateRepository.findById(instance.getTemplateId())
                .orElseThrow(() -> new EntityNotFoundException("AgentTemplate not found: " + instance.getTemplateId()));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<AgentRunEntity> todayRuns = runRepository.findByInstanceIdAndStartedAtBetween(
                instanceId, startOfDay, endOfDay);

        int tokensUsedToday = todayRuns.stream()
                .mapToInt(AgentRunEntity::getTokensUsed)
                .sum();

        int dailyBudget = template.getDailyTokenBudget();
        int remaining = dailyBudget - tokensUsedToday;

        return new BudgetCheckResult(instanceId, dailyBudget, tokensUsedToday, Math.max(0, remaining));
    }

    public record BudgetCheckResult(
            UUID instanceId,
            int dailyBudget,
            int tokensUsedToday,
            int tokensRemaining
    ) {}
}
