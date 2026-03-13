package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.agentinfra.AgentRunStatus;
import com.owlsburg.ops.agentinfra.AgentStepType;
import com.owlsburg.ops.agentinfra.BudgetExceededException;
import com.owlsburg.ops.agentinfra.TriggerType;
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
public class SystemAgentRunService {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentRunService.class);

    private final SystemAgentRunRepository runRepository;
    private final SystemAgentRunStepRepository stepRepository;
    private final SystemAgentInstanceRepository instanceRepository;
    private final SystemAgentTemplateRepository templateRepository;

    public SystemAgentRunService(SystemAgentRunRepository runRepository,
                                  SystemAgentRunStepRepository stepRepository,
                                  SystemAgentInstanceRepository instanceRepository,
                                  SystemAgentTemplateRepository templateRepository) {
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.instanceRepository = instanceRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public SystemAgentRunEntity findById(UUID id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("System-Agent-Run nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<SystemAgentRunStepEntity> findStepsByRunId(UUID runId) {
        return stepRepository.findByRunIdOrderByStepNumber(runId);
    }

    @Transactional
    public SystemAgentRunEntity startRun(UUID instanceId, TriggerType triggerType,
                                          String triggerSource, String inputContext) {
        instanceRepository.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("System-Agent-Instanz nicht gefunden: " + instanceId));

        SystemAgentRunEntity run = new SystemAgentRunEntity();
        run.setInstanceId(instanceId);
        run.setTriggerType(triggerType);
        run.setTriggerSource(triggerSource);
        run.setInputContext(ensureJson(inputContext));
        run.setStatus(AgentRunStatus.PENDING);
        run.setStartedAt(Instant.now());

        log.info("Starting system agent run for instance {} (trigger={})", instanceId, triggerType);
        return runRepository.save(run);
    }

    @Transactional
    public SystemAgentRunEntity startRunWithBudgetCheck(UUID instanceId, TriggerType triggerType,
                                                         String triggerSource, String inputContext) {
        int tokensRemaining = checkBudgetRemaining(instanceId);
        if (tokensRemaining <= 0) {
            throw new BudgetExceededException("System-Agent Token-Budget für heute erschöpft.");
        }
        return startRun(instanceId, triggerType, triggerSource, inputContext);
    }

    @Transactional
    public SystemAgentRunStepEntity addStep(UUID runId, AgentStepType type, String toolName,
                                             String input, String output, int tokensUsed, int durationMs) {
        List<SystemAgentRunStepEntity> existingSteps = stepRepository.findByRunIdOrderByStepNumber(runId);
        int nextStepNumber = existingSteps.isEmpty() ? 1 : existingSteps.getLast().getStepNumber() + 1;

        SystemAgentRunStepEntity step = new SystemAgentRunStepEntity();
        step.setRunId(runId);
        step.setStepNumber(nextStepNumber);
        step.setType(type);
        step.setToolName(toolName);
        step.setInput(input);
        step.setOutput(output);
        step.setTokensUsed(tokensUsed);
        step.setDurationMs(durationMs);

        return stepRepository.save(step);
    }

    @Transactional
    public SystemAgentRunEntity completeRun(UUID runId, String output, int totalTokensUsed, BigDecimal costUsd) {
        SystemAgentRunEntity run = findById(runId);
        run.setStatus(AgentRunStatus.SUCCESS);
        run.setOutput(ensureJson(output));
        run.setTokensUsed(totalTokensUsed);
        run.setCostUsd(costUsd);
        run.setCompletedAt(Instant.now());

        log.info("Completed system agent run {} (tokens={}, cost={})", runId, totalTokensUsed, costUsd);
        return runRepository.save(run);
    }

    @Transactional
    public SystemAgentRunEntity failRun(UUID runId, String errorMessage) {
        SystemAgentRunEntity run = findById(runId);
        run.setStatus(AgentRunStatus.FAILED);
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(Instant.now());
        return runRepository.save(run);
    }

    private int checkBudgetRemaining(UUID instanceId) {
        SystemAgentInstanceEntity instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("System-Agent nicht gefunden: " + instanceId));

        SystemAgentTemplateEntity template = templateRepository.findById(instance.getTemplateId())
                .orElseThrow(() -> new EntityNotFoundException("System-Template nicht gefunden: " + instance.getTemplateId()));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<SystemAgentRunEntity> todayRuns = runRepository.findByInstanceIdAndStartedAtBetween(
                instanceId, startOfDay, endOfDay);

        int tokensUsedToday = todayRuns.stream()
                .mapToInt(SystemAgentRunEntity::getTokensUsed)
                .sum();

        return template.getDailyTokenBudget() - tokensUsedToday;
    }

    private String ensureJson(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"")) {
            return trimmed;
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
