package com.owlsburg.ops.auth.notifications;

import com.owlsburg.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_notification_settings")
@Getter
@Setter
@NoArgsConstructor
public class UserNotificationSettingsEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "agent_run_completed", nullable = false)
    private boolean agentRunCompleted = true;

    @Column(name = "agent_run_failed", nullable = false)
    private boolean agentRunFailed = true;

    @Column(name = "stock_below_minimum", nullable = false)
    private boolean stockBelowMinimum = true;

    @Column(name = "machine_incident", nullable = false)
    private boolean machineIncident = true;

    @Column(name = "job_overdue", nullable = false)
    private boolean jobOverdue = true;

    @Column(name = "absence_request", nullable = false)
    private boolean absenceRequest = true;

    @Column(name = "inbox_new_message", nullable = false)
    private boolean inboxNewMessage = true;

    @Column(name = "in_app", nullable = false)
    private boolean inApp = true;

    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = false;
}
