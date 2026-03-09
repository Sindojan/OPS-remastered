package com.owlsburg.ops.agentinfra;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentRunServiceTest {

    @Mock
    private AgentRunRepository runRepository;

    @Mock
    private AgentRunStepRepository stepRepository;

    @Mock
    private AgentInstanceRepository instanceRepository;

    @Mock
    private AgentTemplateRepository templateRepository;

    @InjectMocks
    private AgentRunService agentRunService;

    private UUID instanceId;
    private UUID templateId;
    private AgentInstanceEntity testInstance;
    private AgentTemplateEntity testTemplate;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        templateId = UUID.randomUUID();

        testInstance = new AgentInstanceEntity();
        testInstance.setId(instanceId);
        testInstance.setTemplateId(templateId);

        testTemplate = new AgentTemplateEntity();
        testTemplate.setId(templateId);
        testTemplate.setDailyTokenBudget(100000);
    }

    @Test
    void startRun_createsRunWithPendingStatus() {
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(testInstance));
        when(runRepository.save(any())).thenAnswer(inv -> {
            AgentRunEntity run = inv.getArgument(0);
            run.setId(UUID.randomUUID());
            return run;
        });

        AgentRunEntity run = agentRunService.startRun(
                instanceId, TriggerType.CHAT, "test", "{\"input\":\"test\"}");

        assertNotNull(run);
        assertEquals(AgentRunStatus.PENDING, run.getStatus());
        assertEquals(instanceId, run.getInstanceId());
        assertNotNull(run.getStartedAt());
        verify(runRepository).save(any());
    }

    @Test
    void startRun_throwsWhenInstanceNotFound() {
        when(instanceRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                agentRunService.startRun(UUID.randomUUID(), TriggerType.CHAT, "test", null));
    }

    @Test
    void completeRun_setsSuccessStatusAndTokens() {
        UUID runId = UUID.randomUUID();
        AgentRunEntity run = new AgentRunEntity();
        run.setId(runId);
        run.setStatus(AgentRunStatus.PENDING);
        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AgentRunEntity completed = agentRunService.completeRun(runId, "output", 5000, new BigDecimal("0.05"));

        assertEquals(AgentRunStatus.SUCCESS, completed.getStatus());
        assertEquals(5000, completed.getTokensUsed());
        assertEquals(new BigDecimal("0.05"), completed.getCostUsd());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    void failRun_setsFailedStatusAndError() {
        UUID runId = UUID.randomUUID();
        AgentRunEntity run = new AgentRunEntity();
        run.setId(runId);
        run.setStatus(AgentRunStatus.PENDING);
        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AgentRunEntity failed = agentRunService.failRun(runId, "Something went wrong");

        assertEquals(AgentRunStatus.FAILED, failed.getStatus());
        assertEquals("Something went wrong", failed.getErrorMessage());
        assertNotNull(failed.getCompletedAt());
    }

    @Test
    void cancelRun_setsCancelledStatus() {
        UUID runId = UUID.randomUUID();
        AgentRunEntity run = new AgentRunEntity();
        run.setId(runId);
        run.setStatus(AgentRunStatus.PENDING);
        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AgentRunEntity cancelled = agentRunService.cancelRun(runId);

        assertEquals(AgentRunStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void startRunWithBudgetCheck_throwsWhenBudgetExhausted() {
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(testInstance));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

        // Simulate runs that used all budget
        AgentRunEntity pastRun = new AgentRunEntity();
        pastRun.setTokensUsed(100000);
        when(runRepository.findByInstanceIdAndStartedAtBetween(eq(instanceId), any(), any()))
                .thenReturn(List.of(pastRun));

        assertThrows(BudgetExceededException.class, () ->
                agentRunService.startRunWithBudgetCheck(instanceId, TriggerType.CHAT, "test", null));
    }

    @Test
    void startRunWithBudgetCheck_succeedsWithRemainingBudget() {
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(testInstance));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(runRepository.findByInstanceIdAndStartedAtBetween(eq(instanceId), any(), any()))
                .thenReturn(List.of());
        when(runRepository.save(any())).thenAnswer(inv -> {
            AgentRunEntity run = inv.getArgument(0);
            run.setId(UUID.randomUUID());
            return run;
        });

        AgentRunEntity run = agentRunService.startRunWithBudgetCheck(
                instanceId, TriggerType.CHAT, "test", null);

        assertNotNull(run);
        verify(runRepository).save(any());
    }

    @Test
    void checkBudget_calculatesCorrectRemaining() {
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(testInstance));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

        AgentRunEntity pastRun = new AgentRunEntity();
        pastRun.setTokensUsed(30000);
        when(runRepository.findByInstanceIdAndStartedAtBetween(eq(instanceId), any(), any()))
                .thenReturn(List.of(pastRun));

        AgentRunService.BudgetCheckResult result = agentRunService.checkBudget(instanceId);

        assertEquals(100000, result.dailyBudget());
        assertEquals(30000, result.tokensUsedToday());
        assertEquals(70000, result.tokensRemaining());
    }

    @Test
    void addStep_assignsSequentialStepNumbers() {
        UUID runId = UUID.randomUUID();
        AgentRunEntity run = new AgentRunEntity();
        run.setId(runId);
        when(runRepository.findById(runId)).thenReturn(Optional.of(run));

        AgentRunStepEntity existingStep = new AgentRunStepEntity();
        existingStep.setStepNumber(1);
        when(stepRepository.findByRunIdOrderByStepNumber(runId)).thenReturn(List.of(existingStep));
        when(stepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AgentRunStepEntity step = agentRunService.addStep(
                runId, AgentStepType.TOOL_CALL, "get_jobs", "{}", "{}", 100, 50);

        assertEquals(2, step.getStepNumber());
        assertEquals("get_jobs", step.getToolName());
    }
}
