package com.owlsburg.ops.auth.notifications;

public record NotificationSettingsUpdateRequest(
        boolean agentRunCompleted,
        boolean agentRunFailed,
        boolean stockBelowMinimum,
        boolean machineIncident,
        boolean jobOverdue,
        boolean absenceRequest,
        boolean inboxNewMessage,
        boolean inApp,
        boolean emailNotifications
) {}
