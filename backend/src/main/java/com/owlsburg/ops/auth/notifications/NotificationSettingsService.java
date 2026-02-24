package com.owlsburg.ops.auth.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationSettingsService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSettingsService.class);

    private final UserNotificationSettingsRepository repository;

    public NotificationSettingsService(UserNotificationSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserNotificationSettingsEntity getForUser(UUID userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating default notification settings for user: {}", userId);
                    UserNotificationSettingsEntity entity = new UserNotificationSettingsEntity();
                    entity.setUserId(userId);
                    return repository.save(entity);
                });
    }

    @Transactional
    public UserNotificationSettingsEntity updateForUser(UUID userId, NotificationSettingsUpdateRequest request) {
        UserNotificationSettingsEntity entity = getForUser(userId);

        entity.setAgentRunCompleted(request.agentRunCompleted());
        entity.setAgentRunFailed(request.agentRunFailed());
        entity.setStockBelowMinimum(request.stockBelowMinimum());
        entity.setMachineIncident(request.machineIncident());
        entity.setJobOverdue(request.jobOverdue());
        entity.setAbsenceRequest(request.absenceRequest());
        entity.setInboxNewMessage(request.inboxNewMessage());
        entity.setInApp(request.inApp());
        entity.setEmailNotifications(request.emailNotifications());

        log.info("Updated notification settings for user: {}", userId);
        return repository.save(entity);
    }
}
