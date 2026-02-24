package com.owlsburg.ops.auth.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettingsEntity, UUID> {

    Optional<UserNotificationSettingsEntity> findByUserId(UUID userId);
}
