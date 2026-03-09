package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.AgentActivitySnapshotResponse;
import com.owlsburg.ops.agentinfra.runtime.AgentActivityBus;
import com.owlsburg.ops.agentinfra.runtime.AgentActivityEvent;
import com.owlsburg.ops.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/agent-activity")
public class AgentActivityController {

    private static final Logger log = LoggerFactory.getLogger(AgentActivityController.class);
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private final AgentActivityService activityService;
    private final AgentActivityBus activityBus;

    public AgentActivityController(AgentActivityService activityService, AgentActivityBus activityBus) {
        this.activityService = activityService;
        this.activityBus = activityBus;
    }

    @GetMapping("/stream")
    public SseEmitter streamActivity() {
        String tenantId = TenantContext.getCurrentTenant();
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout, frontend reconnects
        AtomicBoolean done = new AtomicBoolean(false);

        emitter.onCompletion(() -> done.set(true));
        emitter.onTimeout(() -> done.set(true));
        emitter.onError(e -> done.set(true));

        Thread.startVirtualThread(() -> {
            LinkedBlockingQueue<AgentActivityEvent> queue = new LinkedBlockingQueue<>(200);
            AgentActivityBus.Registration reg = activityBus.subscribe(tenantId, event -> {
                queue.offer(event); // drop if full
            });

            try {
                TenantContext.setCurrentTenant(tenantId);
                while (!done.get()) {
                    // Send snapshot as named SSE event
                    try {
                        AgentActivitySnapshotResponse snapshot = activityService.getSnapshot();
                        String json = objectMapper.writeValueAsString(snapshot);
                        emitter.send(SseEmitter.event()
                                .name("snapshot")
                                .data(json, MediaType.APPLICATION_JSON));
                    } catch (IOException e) {
                        log.debug("SSE snapshot send failed (client disconnected): {}", e.getMessage());
                        break;
                    } catch (Exception e) {
                        log.warn("Activity snapshot error: {}", e.getMessage());
                        // Non-IO error (e.g. DB issue) — wait and retry, don't kill connection
                        try { Thread.sleep(5000); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }

                    // Drain bus events for up to 5 seconds
                    long deadline = System.currentTimeMillis() + 5000;
                    while (!done.get() && System.currentTimeMillis() < deadline) {
                        long remaining = deadline - System.currentTimeMillis();
                        if (remaining <= 0) break;
                        try {
                            AgentActivityEvent event = queue.poll(remaining, TimeUnit.MILLISECONDS);
                            if (event == null) continue;
                            String eventJson = objectMapper.writeValueAsString(new ActivityEventDto(
                                    event.type().name(),
                                    event.agentInstanceId() != null ? event.agentInstanceId().toString() : null,
                                    event.targetInstanceId() != null ? event.targetInstanceId().toString() : null,
                                    event.agentName(),
                                    event.detail(),
                                    event.timestamp().toString()
                            ));
                            emitter.send(SseEmitter.event()
                                    .name("activity")
                                    .data(eventJson, MediaType.APPLICATION_JSON));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (IOException e) {
                            log.debug("SSE activity send failed (client disconnected): {}", e.getMessage());
                            done.set(true);
                            break;
                        } catch (Exception e) {
                            log.debug("Activity event serialization error: {}", e.getMessage());
                            // Skip this event, continue loop
                        }
                    }
                }
            } finally {
                reg.unsubscribe();
                TenantContext.clear();
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }

    private record ActivityEventDto(
            String type,
            String agentInstanceId,
            String targetInstanceId,
            String agentName,
            String detail,
            String timestamp
    ) {}
}
