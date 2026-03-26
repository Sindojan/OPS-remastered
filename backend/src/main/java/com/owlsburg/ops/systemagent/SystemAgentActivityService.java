package com.owlsburg.ops.systemagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.AgentRunStatus;
import com.owlsburg.ops.agentinfra.dto.ActiveLinkDto;
import com.owlsburg.ops.agentinfra.dto.AgentActivitySnapshotResponse;
import com.owlsburg.ops.agentinfra.dto.AgentInstanceActivity;
import com.owlsburg.ops.systemagent.messaging.SystemAgentMessageEntity;
import com.owlsburg.ops.systemagent.messaging.SystemAgentMessageRepository;
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
public class SystemAgentActivityService {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentActivityService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SystemAgentInstanceService instanceService;
    private final SystemAgentTemplateRepository templateRepository;
    private final SystemAgentRunRepository runRepository;
    private final SystemAgentMessageRepository messageRepository;
    private final SystemChatSessionRepository chatSessionRepository;
    private final SystemChatMessageRepository chatMessageRepository;

    public SystemAgentActivityService(SystemAgentInstanceService instanceService,
                                       SystemAgentTemplateRepository templateRepository,
                                       SystemAgentRunRepository runRepository,
                                       SystemAgentMessageRepository messageRepository,
                                       SystemChatSessionRepository chatSessionRepository,
                                       SystemChatMessageRepository chatMessageRepository) {
        this.instanceService = instanceService;
        this.templateRepository = templateRepository;
        this.runRepository = runRepository;
        this.messageRepository = messageRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional(readOnly = true)
    public AgentActivitySnapshotResponse getSnapshot() {
        List<SystemAgentInstanceEntity> instances = instanceService.findAll();

        Map<UUID, SystemAgentTemplateEntity> templateCache = new HashMap<>();

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now = Instant.now();
        Instant fiveMinutesAgo = now.minusSeconds(300);

        // Load recent chat messages to detect activity
        List<SystemChatMessageEntity> recentChatMessages = chatMessageRepository
                .findByCreatedAtAfterOrderByCreatedAtDesc(fiveMinutesAgo);

        // Build session -> agentInstanceId mapping for recent sessions (batch query to avoid N+1)
        Set<UUID> recentSessionIds = recentChatMessages.stream()
                .map(SystemChatMessageEntity::getSessionId)
                .collect(Collectors.toSet());
        Map<UUID, UUID> sessionToAgent = new HashMap<>();
        if (!recentSessionIds.isEmpty()) {
            chatSessionRepository.findByIdIn(recentSessionIds)
                    .forEach(s -> sessionToAgent.put(s.getId(), s.getAgentInstanceId()));
        }

        // Per agent: latest user message + latest activity timestamp from chat
        Map<UUID, String> agentCurrentTask = new HashMap<>();
        Map<UUID, Instant> agentLastChatActivity = new HashMap<>();
        for (SystemChatMessageEntity msg : recentChatMessages) {
            UUID agentId = sessionToAgent.get(msg.getSessionId());
            if (agentId == null) continue;

            agentLastChatActivity.merge(agentId, msg.getCreatedAt(),
                    (existing, candidate) -> existing.isAfter(candidate) ? existing : candidate);

            if ("user".equals(msg.getRole()) && !agentCurrentTask.containsKey(agentId)) {
                String content = msg.getContent();
                if (content.length() > 120) content = content.substring(0, 120) + "\u2026";
                agentCurrentTask.put(agentId, content);
            }
        }

        List<AgentInstanceActivity> activities = new ArrayList<>();
        for (SystemAgentInstanceEntity instance : instances) {
            SystemAgentTemplateEntity template = templateCache.computeIfAbsent(
                    instance.getTemplateId(),
                    id -> templateRepository.findById(id).orElse(null)
            );

            String templateRole = template != null ? template.getRole() : "unknown";
            int dailyTokenBudget = template != null ? template.getDailyTokenBudget() : 100000;

            String model = parseModelFromConfig(instance.getConfig());

            Optional<SystemAgentRunEntity> latestRun = runRepository
                    .findTopByInstanceIdOrderByStartedAtDesc(instance.getId());

            String currentTask = null;
            UUID activeRunId = instance.getLastRunId();
            Instant lastActivityAt = instance.getActivityStatusChangedAt();

            if (latestRun.isPresent()) {
                SystemAgentRunEntity run = latestRun.get();
                if (run.getStatus() == AgentRunStatus.RUNNING || run.getStatus() == AgentRunStatus.PENDING) {
                    currentTask = run.getInputContext();
                    activeRunId = run.getId();
                }
                Instant runTime = run.getCompletedAt() != null ? run.getCompletedAt() : run.getStartedAt();
                if (runTime != null && (lastActivityAt == null || runTime.isAfter(lastActivityAt))) {
                    lastActivityAt = runTime;
                }
            }

            // Override with chat activity if more recent
            Instant chatActivity = agentLastChatActivity.get(instance.getId());
            if (chatActivity != null) {
                if (lastActivityAt == null || chatActivity.isAfter(lastActivityAt)) {
                    lastActivityAt = chatActivity;
                }
                if (currentTask == null) {
                    currentTask = agentCurrentTask.get(instance.getId());
                }
            }

            List<SystemAgentRunEntity> todayRuns = runRepository
                    .findByInstanceIdAndStartedAtBetween(instance.getId(), startOfDay, now);
            int tokensUsedToday = todayRuns.stream().mapToInt(SystemAgentRunEntity::getTokensUsed).sum();

            activities.add(new AgentInstanceActivity(
                    instance.getId(),
                    instance.getName(),
                    templateRole,
                    instance.getStatus().name(),
                    instance.getActivityStatus().name(),
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
        List<SystemAgentMessageEntity> recentAgentMessages = messageRepository.findByCreatedAtAfter(fiveMinutesAgo);

        Set<UUID> instanceIds = instances.stream()
                .map(SystemAgentInstanceEntity::getId)
                .collect(Collectors.toSet());

        Map<String, ActiveLinkDto> linkMap = new LinkedHashMap<>();
        for (SystemAgentMessageEntity msg : recentAgentMessages) {
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
