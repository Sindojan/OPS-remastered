package com.owlsburg.ops.auth.notifications;

public record NotificationSettingsResponse(
        boolean agentRunCompleted,
        boolean agentRunFailed,
        boolean stockBelowMinimum,
        boolean machineIncident,
        boolean jobOverdue,
        boolean absenceRequest,
        boolean inboxNewMessage,
        boolean inApp,
        boolean emailNotifications
) {
    public static NotificationSettingsResponse from(UserNotificationSettingsEntity entity) {
        return new NotificationSettingsResponse(
                entity.isAgentRunCompleted(),
                entity.isAgentRunFailed(),
                entity.isStockBelowMinimum(),
                entity.isMachineIncident(),
                entity.isJobOverdue(),
                entity.isAbsenceRequest(),
                entity.isInboxNewMessage(),
                entity.isInApp(),
                entity.isEmailNotifications()
        );
    }
}
