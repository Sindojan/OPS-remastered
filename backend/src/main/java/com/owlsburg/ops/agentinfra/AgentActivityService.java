package com.owlsburg.ops.agentinfra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.dto.ActiveLinkDto;
import com.owlsburg.ops.agentinfra.dto.AgentActivitySnapshotResponse;
import com.owlsburg.ops.agentinfra.dto.AgentInstanceActivity;
import com.owlsburg.ops.agentinfra.messaging.AgentMessageEntity;
import com.owlsburg.ops.agentinfra.messaging.AgentMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgentActivityService {

    private static final Logger log = LoggerFactory.getLogger(AgentActivityService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AgentInstanceRepository instanceRepository;
    private final AgentTemplateRepository templateRepository;
    private final AgentRunRepository runRepository;
    private final AgentMessageRepository messageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public AgentActivityService(AgentInstanceRepository instanceRepository,
                                AgentTemplateRepository templateRepository,
                                AgentRunRepository runRepository,
                                AgentMessageRepository messageRepository,
                                ChatSessionRepository chatSessionRepository,
                                ChatMessageRepository chatMessageRepository) {
        this.instanceRepository = instanceRepository;
        this.templateRepository = templateRepository;
        this.runRepository = runRepository;
        this.messageRepository = messageRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional(readOnly = true)
    public AgentActivitySnapshotResponse getSnapshot() {
        List<AgentInstanceEntity> instances = instanceRepository.findAll();

        Map<UUID, AgentTemplateEntity> templateCache = new HashMap<>();

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now = Instant.now();
        Instant fiveMinutesAgo = now.minusSeconds(300);

        // Load recent chat messages to detect activity
        List<ChatMessageEntity> recentChatMessages = chatMessageRepository
                .findByCreatedAtAfterOrderByCreatedAtDesc(fiveMinutesAgo);

        // Build session → agentInstanceId mapping for recent sessions
        Set<UUID> recentSessionIds = recentChatMessages.stream()
                .map(ChatMessageEntity::getSessionId)
                .collect(Collectors.toSet());
        Map<UUID, UUID> sessionToAgent = new HashMap<>();
        for (UUID sessionId : recentSessionIds) {
            chatSessionRepository.findById(sessionId)
                    .ifPresent(s -> sessionToAgent.put(s.getId(), s.getAgentInstanceId()));
        }

        // Per agent: latest user message + latest activity timestamp from chat
        Map<UUID, String> agentCurrentTask = new HashMap<>();
        Map<UUID, Instant> agentLastChatActivity = new HashMap<>();
        for (ChatMessageEntity msg : recentChatMessages) {
            UUID agentId = sessionToAgent.get(msg.getSessionId());
            if (agentId == null) continue;

            // Track latest activity (any role)
            agentLastChatActivity.merge(agentId, msg.getCreatedAt(),
                    (existing, candidate) -> existing.isAfter(candidate) ? existing : candidate);

            // Use latest user message as current task
            if ("user".equals(msg.getRole()) && !agentCurrentTask.containsKey(agentId)) {
                String content = msg.getContent();
                if (content.length() > 120) content = content.substring(0, 120) + "…";
                agentCurrentTask.put(agentId, content);
            }
        }

        List<AgentInstanceActivity> activities = new ArrayList<>();
        for (AgentInstanceEntity instance : instances) {
            AgentTemplateEntity template = templateCache.computeIfAbsent(
                    instance.getTemplateId(),
                    id -> templateRepository.findById(id).orElse(null)
            );

            String templateRole = template != null ? template.getRole() : "unknown";
            int dailyTokenBudget = template != null ? template.getDailyTokenBudget() : 100000;

            String model = parseModelFromConfig(instance.getConfig());

            // Check agent_runs for active tasks
            Optional<AgentRunEntity> latestRun = runRepository
                    .findTopByInstanceIdOrderByStartedAtDesc(instance.getId());

            String currentTask = null;
            UUID activeRunId = null;
            Instant lastActivityAt = null;

            if (latestRun.isPresent()) {
                AgentRunEntity run = latestRun.get();
                if (run.getStatus() == AgentRunStatus.RUNNING || run.getStatus() == AgentRunStatus.PENDING) {
                    currentTask = run.getInputContext();
                    activeRunId = run.getId();
                }
                // Use run timestamp as last activity
                Instant runTime = run.getCompletedAt() != null ? run.getCompletedAt() : run.getStartedAt();
                if (runTime != null) {
                    lastActivityAt = runTime;
                }
            }

            // Override with chat activity if more recent
            Instant chatActivity = agentLastChatActivity.get(instance.getId());
            if (chatActivity != null) {
                if (lastActivityAt == null || chatActivity.isAfter(lastActivityAt)) {
                    lastActivityAt = chatActivity;
                }
                // Use chat task if no active run task
                if (currentTask == null) {
                    currentTask = agentCurrentTask.get(instance.getId());
                }
            }

            List<AgentRunEntity> todayRuns = runRepository
                    .findByInstanceIdAndStartedAtBetween(instance.getId(), startOfDay, now);
            int tokensUsedToday = todayRuns.stream().mapToInt(AgentRunEntity::getTokensUsed).sum();

            activities.add(new AgentInstanceActivity(
                    instance.getId(),
                    instance.getName(),
                    templateRole,
                    instance.getStatus().name(),
                    instance.getParentInstanceId(),
                    model,
                    instance.getType().name(),
                    currentTask,
                    tokensUsedToday,
                    dailyTokenBudget,
                    activeRunId,
                    lastActivityAt
            ));
        }

        // Active links from agent_messages (inter-agent communication)
        List<AgentMessageEntity> recentAgentMessages = messageRepository.findByCreatedAtAfter(fiveMinutesAgo);

        Set<UUID> instanceIds = instances.stream()
                .map(AgentInstanceEntity::getId)
                .collect(Collectors.toSet());

        Map<String, ActiveLinkDto> linkMap = new LinkedHashMap<>();
        for (AgentMessageEntity msg : recentAgentMessages) {
            if (instanceIds.contains(msg.getSenderInstanceId()) && instanceIds.contains(msg.getTargetInstanceId())) {
                String key = msg.getSenderInstanceId() + "->" + msg.getTargetInstanceId();
                ActiveLinkDto existing = linkMap.get(key);
                if (existing == null || msg.getCreatedAt().isAfter(existing.lastMessageAt())) {
                    linkMap.put(key, new ActiveLinkDto(
                            msg.getSenderInstanceId(),
                            msg.getTargetInstanceId(),
                            msg.getCreatedAt(),
                            msg.getMessageType()
                    ));
                }
            }
        }

        return new AgentActivitySnapshotResponse(activities, new ArrayList<>(linkMap.values()));
    }

    private String parseModelFromConfig(String configJson) {
        if (configJson == null || configJson.isBlank() || configJson.equals("{}")) {
            return "claude-sonnet-4-6";
        }
        try {
            JsonNode node = objectMapper.readTree(configJson);
            JsonNode modelNode = node.get("model");
            if (modelNode != null && !modelNode.isNull()) {
                return modelNode.asText();
            }
        } catch (Exception e) {
            log.debug("Failed to parse model from config: {}", e.getMessage());
        }
        return "claude-sonnet-4-6";
    }
}
