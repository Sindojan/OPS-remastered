package com.owlsburg.ops.systemagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.AgentActivityStatus;
import com.owlsburg.ops.agentinfra.AgentRunStatus;
import com.owlsburg.ops.agentinfra.BudgetExceededException;
import com.owlsburg.ops.agentinfra.TriggerType;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
import com.owlsburg.ops.systemagent.dto.SystemChatMessageResponse;
import com.owlsburg.ops.systemagent.dto.SystemChatRequest;
import com.owlsburg.ops.systemagent.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SystemChatService {

    private static final Logger log = LoggerFactory.getLogger(SystemChatService.class);

    // Default System CEO instance ID (well-known from V23 seed)
    private static final UUID SYSTEM_CEO_INSTANCE_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    private final SystemAgentInstanceService instanceService;
    private final SystemChatSessionService chatSessionService;
    private final SystemAgentFactory agentFactory;
    private final ObjectMapper objectMapper;
    private final SystemAgentActivityBus activityBus;
    private final SystemAgentRunService runService;
    private final SystemAgentIncidentService incidentService;

    public SystemChatService(SystemAgentInstanceService instanceService,
                              SystemChatSessionService chatSessionService,
                              SystemAgentFactory agentFactory,
                              ObjectMapper objectMapper,
                              SystemAgentActivityBus activityBus,
                              SystemAgentRunService runService,
                              SystemAgentIncidentService incidentService) {
        this.instanceService = instanceService;
        this.chatSessionService = chatSessionService;
        this.agentFactory = agentFactory;
        this.objectMapper = objectMapper;
        this.activityBus = activityBus;
        this.runService = runService;
        this.incidentService = incidentService;
    }

    public UUID streamChat(SystemChatRequest request, UUID userId, SseEmitter emitter) {
        UUID sessionId = null;
        UUID agentRunId = null;
        UUID instanceId = request.agentInstanceId() != null ? request.agentInstanceId() : SYSTEM_CEO_INSTANCE_ID;
        boolean activitySet = false;
        try {
            // 1. Resolve or create session
            if (request.sessionId() != null) {
                sessionId = request.sessionId();
            } else {
                SystemChatSessionEntity session = chatSessionService.createSession(userId, instanceId);
                sessionId = session.getId();
            }

            // 2. Load agent instance
            SystemAgentInstanceEntity instance = instanceService.findById(instanceId);

            // 3. Save greeting on new session
            if (request.sessionId() == null) {
                String greeting = "Guten Tag! Ich bin der " + instance.getName() + ". Wie kann ich Ihnen helfen?";
                chatSessionService.saveMessage(sessionId, "assistant", greeting);
            }

            // 4. Save user message to DB
            chatSessionService.saveMessage(sessionId, "user", request.message());

            // 5. Send sessionId as first SSE event
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                    Map.of("sessionId", sessionId.toString()))));

            // 6. Create Agent via Factory
            SystemAgent agent = agentFactory.createAgent(instanceId);

            // 7. Load full history from DB and build messages
            List<SystemChatMessageResponse> dbHistory = chatSessionService.getMessages(sessionId);
            List<ObjectNode> messages = new ArrayList<>();
            for (SystemChatMessageResponse msg : dbHistory) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
                messages.add(msgNode);
            }

            // 8. Create AgentRun for tracking (with budget check)
            SystemAgentRunEntity agentRun;
            try {
                agentRun = runService.startRunWithBudgetCheck(
                        instanceId, TriggerType.CHAT,
                        "system_chat:" + sessionId, request.message());
            } catch (BudgetExceededException e) {
                sendErrorAndComplete(emitter, "Token-Budget für heute erschöpft.");
                return sessionId;
            }
            agentRunId = agentRun.getId();

            // Set BUSY before execution
            safeUpdateActivity(instanceId, AgentActivityStatus.BUSY);
            activitySet = true;

            // 9. Execute streaming via SystemCeoAgent
            SystemAgentContext context = SystemAgentContext.forChat(userId, sessionId, activityBus);

            String fullResponse;
            int inputTokens = 0;
            int outputTokens = 0;
            if (agent instanceof SystemCeoAgent ceoAgent) {
                SystemCeoAgent.ChatResult chatResult = ceoAgent.getLastResponse(context, request.message(), messages, emitter);
                fullResponse = chatResult.response();
                inputTokens = chatResult.inputTokens();
                outputTokens = chatResult.outputTokens();

                if (chatResult.iterationsExhausted()) {
                    reportIncidentSafe(instanceId, "MAX_ITERATIONS",
                            "System CEO Agent erreichte maximale Iterationen ohne natürliches Ende");
                }
            } else {
                // Non-CEO system agents: execute synchronously
                SystemAgentResult result = agent.execute(context, request.message());
                fullResponse = result.output();
                if (fullResponse != null && !fullResponse.isEmpty()) {
                    emitter.send(SseEmitter.event().data(
                            "{\"token\":" + objectMapper.writeValueAsString(fullResponse) + "}"));
                }
            }

            // 10. Complete AgentRun with token tracking
            String model = resolveModel(instance);
            int totalTokens = inputTokens + outputTokens;
            BigDecimal cost = calculateCost(model, inputTokens, outputTokens);
            runService.completeRun(agentRun.getId(), fullResponse, totalTokens, cost);

            // Set IDLE + link last run on success
            safeUpdateActivity(instanceId, AgentActivityStatus.IDLE);
            safeLinkLastRun(instanceId, agentRun.getId());

            // 11. Save assistant message to DB
            if (fullResponse != null && !fullResponse.isEmpty()) {
                chatSessionService.saveMessage(sessionId, "assistant", fullResponse);
            }

            // 12. Send usage event
            if (inputTokens > 0 || outputTokens > 0) {
                try {
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                            Map.of("usage", Map.of("inputTokens", inputTokens, "outputTokens", outputTokens)))));
                } catch (Exception e) {
                    log.debug("SSE usage event send failed: {}", e.getMessage());
                }
            }

            // 13. Auto-generate title on first message
            if (request.sessionId() == null) {
                String title = request.message().length() > 50
                        ? request.message().substring(0, 50) + "..."
                        : request.message();
                chatSessionService.updateSessionTitle(sessionId, title);
            }

            // 14. Send done event
            try {
                emitter.send(SseEmitter.event().data("{\"done\":true}"));
                emitter.complete();
            } catch (Exception e) {
                log.debug("SSE done event send failed (client likely disconnected): {}", e.getMessage());
            }

            return sessionId;

        } catch (LlmProviderException e) {
            failRunSafe(agentRunId, e.getMessage());
            safeUpdateActivity(instanceId, AgentActivityStatus.ERROR);
            safeLinkLastRun(instanceId, agentRunId);
            reportIncidentSafe(instanceId, "LLM_ERROR", e.getMessage());
            sendErrorAndComplete(emitter, e.getMessage());
            return sessionId;
        } catch (IllegalArgumentException e) {
            failRunSafe(agentRunId, e.getMessage());
            if (activitySet) safeUpdateActivity(instanceId, AgentActivityStatus.ERROR);
            safeLinkLastRun(instanceId, agentRunId);
            sendErrorAndComplete(emitter, e.getMessage());
            return sessionId;
        } catch (Exception e) {
            log.error("System chat streaming error", e);
            failRunSafe(agentRunId, e.getMessage());
            if (activitySet) safeUpdateActivity(instanceId, AgentActivityStatus.ERROR);
            safeLinkLastRun(instanceId, agentRunId);
            reportIncidentSafe(instanceId, "RUNTIME_ERROR", e.getMessage());
            sendErrorAndComplete(emitter, "Interner Fehler");
            return sessionId;
        } finally {
            finalizeRunSafe(agentRunId);
            resetBusyIfNeeded(instanceId);
        }
    }

    private void failRunSafe(UUID runId, String message) {
        if (runId == null) return;
        try {
            runService.failRun(runId, message);
        } catch (Exception e) {
            log.debug("Failed to mark SystemAgentRun as failed: {}", e.getMessage());
        }
    }

    private void finalizeRunSafe(UUID runId) {
        if (runId == null) return;
        try {
            SystemAgentRunEntity run = runService.findById(runId);
            if (run.getStatus() == AgentRunStatus.PENDING || run.getStatus() == AgentRunStatus.RUNNING) {
                runService.failRun(runId, "Run nicht ordnungsgemäß abgeschlossen (SSE-Timeout oder Abbruch)");
            }
        } catch (Exception e) {
            log.debug("Failed to finalize SystemAgentRun: {}", e.getMessage());
        }
    }

    private void safeUpdateActivity(UUID instanceId, AgentActivityStatus status) {
        if (instanceId == null) return;
        try {
            instanceService.updateActivityStatus(instanceId, status);
        } catch (Exception e) {
            log.debug("Failed to update activity status: {}", e.getMessage());
        }
    }

    private void safeLinkLastRun(UUID instanceId, UUID runId) {
        if (instanceId == null || runId == null) return;
        try {
            instanceService.linkLastRun(instanceId, runId);
        } catch (Exception e) {
            log.debug("Failed to link last run: {}", e.getMessage());
        }
    }

    private void resetBusyIfNeeded(UUID instanceId) {
        if (instanceId == null) return;
        try {
            SystemAgentInstanceEntity instance = instanceService.findById(instanceId);
            if (instance.getActivityStatus() == AgentActivityStatus.BUSY) {
                instanceService.updateActivityStatus(instanceId, AgentActivityStatus.IDLE);
            }
        } catch (Exception e) {
            log.debug("Failed to reset BUSY status: {}", e.getMessage());
        }
    }

    private void reportIncidentSafe(UUID instanceId, String type, String description) {
        if (instanceId == null) return;
        try {
            incidentService.report(instanceId, type, description);
        } catch (Exception e) {
            log.debug("Failed to report incident: {}", e.getMessage());
        }
    }

    private String resolveModel(SystemAgentInstanceEntity instance) {
        try {
            var config = objectMapper.readTree(instance.getConfig());
            if (config.has("model")) {
                return config.get("model").asText();
            }
        } catch (Exception e) {
            log.debug("Failed to parse instance config: {}", e.getMessage());
        }
        return "claude-sonnet-4-20250514";
    }

    private BigDecimal calculateCost(String model, int inputTokens, int outputTokens) {
        if (model != null && model.contains("opus")) {
            return BigDecimal.valueOf(inputTokens * 15.0 / 1_000_000 + outputTokens * 75.0 / 1_000_000);
        } else if (model != null && model.contains("haiku")) {
            return BigDecimal.valueOf(inputTokens * 0.80 / 1_000_000 + outputTokens * 4.0 / 1_000_000);
        }
        return BigDecimal.valueOf(inputTokens * 3.0 / 1_000_000 + outputTokens * 15.0 / 1_000_000);
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(
                    "{\"error\":" + objectMapper.writeValueAsString(message) + "}"));
            emitter.complete();
        } catch (Exception ex) {
            log.warn("Failed to send error event: {}", ex.getMessage());
            try {
                emitter.completeWithError(ex);
            } catch (Exception ignored) {
                // emitter already completed
            }
        }
    }
}
