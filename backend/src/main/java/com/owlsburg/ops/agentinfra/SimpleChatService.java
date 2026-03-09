package com.owlsburg.ops.agentinfra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlsburg.ops.agentinfra.dto.ChatMessageResponse;
import com.owlsburg.ops.agentinfra.dto.SimpleChatRequest;
import com.owlsburg.ops.agentinfra.llm.LlmProviderException;
import com.owlsburg.ops.agentinfra.memory.EpisodicMemoryExtractor;
import com.owlsburg.ops.agentinfra.memory.AgentMemoryService;
import com.owlsburg.ops.agentinfra.runtime.*;
import com.owlsburg.ops.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SimpleChatService {

    private static final Logger log = LoggerFactory.getLogger(SimpleChatService.class);

    private final AgentInstanceRepository agentInstanceRepository;
    private final ChatSessionService chatSessionService;
    private final AgentFactory agentFactory;
    private final ObjectMapper objectMapper;
    private final AgentActivityBus activityBus;
    private final AgentRunService agentRunService;
    private final AgentInstanceService agentInstanceService;
    private final AgentIncidentService agentIncidentService;
    private final EpisodicMemoryExtractor episodicMemoryExtractor;
    private final AgentMemoryService agentMemoryService;

    public SimpleChatService(AgentInstanceRepository agentInstanceRepository,
                             ChatSessionService chatSessionService,
                             AgentFactory agentFactory,
                             ObjectMapper objectMapper,
                             AgentActivityBus activityBus,
                             AgentRunService agentRunService,
                             AgentInstanceService agentInstanceService,
                             AgentIncidentService agentIncidentService,
                             EpisodicMemoryExtractor episodicMemoryExtractor,
                             AgentMemoryService agentMemoryService) {
        this.agentInstanceRepository = agentInstanceRepository;
        this.chatSessionService = chatSessionService;
        this.agentFactory = agentFactory;
        this.objectMapper = objectMapper;
        this.activityBus = activityBus;
        this.agentRunService = agentRunService;
        this.agentInstanceService = agentInstanceService;
        this.agentIncidentService = agentIncidentService;
        this.episodicMemoryExtractor = episodicMemoryExtractor;
        this.agentMemoryService = agentMemoryService;
    }

    public UUID streamChat(SimpleChatRequest request, UUID userId, SseEmitter emitter) {
        UUID sessionId = null;
        UUID agentRunId = null;
        UUID instanceId = request.agentInstanceId();
        boolean activitySet = false;
        try {
            // 1. Resolve or create session
            if (request.sessionId() != null) {
                sessionId = request.sessionId();
            } else {
                ChatSessionEntity session = chatSessionService.createSession(userId, request.agentInstanceId());
                sessionId = session.getId();
            }

            // 2. Load agent instance with tenant check (defense-in-depth)
            String tenantId = TenantContext.getCurrentTenant();
            UUID tenantUuid = UUID.fromString(tenantId);
            AgentInstanceEntity instance = agentInstanceRepository.findByIdAndTenantId(instanceId, tenantUuid)
                    .orElseThrow(() -> new AccessDeniedException("Zugriff verweigert"));

            // 3. Save greeting on new session
            if (request.sessionId() == null) {
                String greeting = "Guten Tag! Ich bin Ihr " + instance.getName() + ". Wie kann ich Ihnen helfen?";
                chatSessionService.saveMessage(sessionId, "assistant", greeting);
            }

            // 4. Save user message to DB
            chatSessionService.saveMessage(sessionId, "user", request.message());

            // 5. Send sessionId as first SSE event
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                    Map.of("sessionId", sessionId.toString()))));

            // 6. Create Agent via Factory (builds system prompt, tools, memory injection)
            Agent agent = agentFactory.createAgent(instanceId, tenantId);

            // 7. Load full history from DB and build messages
            List<ChatMessageResponse> dbHistory = chatSessionService.getMessages(sessionId);
            List<ObjectNode> messages = new ArrayList<>();
            for (ChatMessageResponse msg : dbHistory) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
                messages.add(msgNode);
            }

            // 8. Create AgentRun for tracking
            AgentRunEntity agentRun = agentRunService.startRun(
                    instanceId, TriggerType.CHAT,
                    "chat:" + sessionId, request.message());
            agentRunId = agentRun.getId();

            // Set BUSY before execution
            safeUpdateActivity(instanceId, AgentActivityStatus.BUSY);
            activitySet = true;

            // 9. Execute streaming via CeoAgent
            AgentContext context = AgentContext.forChat(tenantId, userId, sessionId, activityBus);

            String fullResponse;
            int inputTokens = 0;
            int outputTokens = 0;
            if (agent instanceof CeoAgent ceoAgent) {
                CeoAgent.ChatResult chatResult = ceoAgent.getLastResponse(context, request.message(), messages, emitter);
                fullResponse = chatResult.response();
                inputTokens = chatResult.inputTokens();
                outputTokens = chatResult.outputTokens();

                // Extract episodic memories and record tool outcomes
                if (chatResult.toolCallLogs() != null && !chatResult.toolCallLogs().isEmpty()) {
                    safeExtractEpisodicMemories(instanceId, chatResult.toolCallLogs());
                    safeRecordToolOutcomes(instanceId, chatResult.toolCallLogs());
                }
            } else {
                // Non-CEO agents: execute synchronously and send result as token
                AgentResult result = agent.execute(context, request.message());
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
            agentRunService.completeRun(agentRun.getId(), fullResponse, totalTokens, cost);

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

            // 13. Auto-generate title on first message (new session)
            if (request.sessionId() == null) {
                String title = request.message().length() > 50
                        ? request.message().substring(0, 50) + "..."
                        : request.message();
                chatSessionService.updateSessionTitle(sessionId, title);
            }

            // 14. Send done event
            emitter.send(SseEmitter.event().data("{\"done\":true}"));
            emitter.complete();

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
            log.error("Chat streaming error", e);
            failRunSafe(agentRunId, e.getMessage());
            if (activitySet) safeUpdateActivity(instanceId, AgentActivityStatus.ERROR);
            safeLinkLastRun(instanceId, agentRunId);
            reportIncidentSafe(instanceId, "RUNTIME_ERROR", e.getMessage());
            sendErrorAndComplete(emitter, "Interner Fehler");
            return sessionId;
        } finally {
            // Guarantee: if run still PENDING/RUNNING, mark FAILED
            finalizeRunSafe(agentRunId);
            // Guarantee: reset BUSY to IDLE if still BUSY (e.g. SSE timeout)
            resetBusyIfNeeded(instanceId);
        }
    }

    private void failRunSafe(UUID runId, String message) {
        if (runId == null) return;
        try {
            agentRunService.failRun(runId, message);
        } catch (Exception e) {
            log.debug("Failed to mark AgentRun as failed: {}", e.getMessage());
        }
    }

    private void finalizeRunSafe(UUID runId) {
        if (runId == null) return;
        try {
            AgentRunEntity run = agentRunService.findById(runId);
            if (run.getStatus() == AgentRunStatus.PENDING || run.getStatus() == AgentRunStatus.RUNNING) {
                agentRunService.failRun(runId, "Run nicht ordnungsgemäß abgeschlossen (SSE-Timeout oder Abbruch)");
            }
        } catch (Exception e) {
            log.debug("Failed to finalize AgentRun: {}", e.getMessage());
        }
    }

    private void safeUpdateActivity(UUID instanceId, AgentActivityStatus status) {
        if (instanceId == null) return;
        try {
            agentInstanceService.updateActivityStatus(instanceId, status);
        } catch (Exception e) {
            log.debug("Failed to update activity status: {}", e.getMessage());
        }
    }

    private void safeLinkLastRun(UUID instanceId, UUID runId) {
        if (instanceId == null || runId == null) return;
        try {
            agentInstanceService.linkLastRun(instanceId, runId);
        } catch (Exception e) {
            log.debug("Failed to link last run: {}", e.getMessage());
        }
    }

    private void resetBusyIfNeeded(UUID instanceId) {
        if (instanceId == null) return;
        try {
            AgentInstanceEntity instance = agentInstanceService.findById(instanceId);
            if (instance.getActivityStatus() == AgentActivityStatus.BUSY) {
                agentInstanceService.updateActivityStatus(instanceId, AgentActivityStatus.IDLE);
            }
        } catch (Exception e) {
            log.debug("Failed to reset BUSY status: {}", e.getMessage());
        }
    }

    private void reportIncidentSafe(UUID instanceId, String type, String description) {
        if (instanceId == null) return;
        try {
            agentIncidentService.report(instanceId, type, description);
        } catch (Exception e) {
            log.debug("Failed to report incident: {}", e.getMessage());
        }
    }

    private String resolveModel(AgentInstanceEntity instance) {
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

    private void safeExtractEpisodicMemories(UUID instanceId, List<CeoAgent.ToolCallLog> toolCallLogs) {
        try {
            List<EpisodicMemoryExtractor.ToolCallRecord> records = toolCallLogs.stream()
                    .map(tcl -> new EpisodicMemoryExtractor.ToolCallRecord(
                            tcl.toolName(), tcl.input(), tcl.result(), tcl.success()))
                    .toList();
            episodicMemoryExtractor.extractFromToolCalls(instanceId, records);
        } catch (Exception e) {
            log.debug("Failed to extract episodic memories: {}", e.getMessage());
        }
    }

    private void safeRecordToolOutcomes(UUID instanceId, List<CeoAgent.ToolCallLog> toolCallLogs) {
        try {
            for (CeoAgent.ToolCallLog tcl : toolCallLogs) {
                agentMemoryService.recordToolOutcome(instanceId, tcl.toolName(), tcl.success());
            }
        } catch (Exception e) {
            log.debug("Failed to record tool outcomes: {}", e.getMessage());
        }
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
