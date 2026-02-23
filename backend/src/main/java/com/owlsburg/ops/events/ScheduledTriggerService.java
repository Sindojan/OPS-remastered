package com.owlsburg.ops.events;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduledTriggerService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTriggerService.class);

    private final ScheduledTriggerRepository triggerRepository;

    public ScheduledTriggerService(ScheduledTriggerRepository triggerRepository) {
        this.triggerRepository = triggerRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduledTriggerEntity> findAll() {
        return triggerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ScheduledTriggerEntity findById(UUID id) {
        return triggerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ScheduledTrigger not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ScheduledTriggerEntity> findByInstanceId(UUID instanceId) {
        return triggerRepository.findByInstanceId(instanceId);
    }

    @Transactional
    public ScheduledTriggerEntity create(ScheduledTriggerEntity trigger) {
        trigger.setActive(true);
        trigger.setNextRunAt(calculateNextRunAt(trigger.getCronExpression()));
        log.info("Creating scheduled trigger for instance {} (cron={})",
                trigger.getInstanceId(), trigger.getCronExpression());
        return triggerRepository.save(trigger);
    }

    @Transactional
    public ScheduledTriggerEntity update(UUID id, ScheduledTriggerEntity updated) {
        ScheduledTriggerEntity existing = findById(id);
        existing.setInstanceId(updated.getInstanceId());
        existing.setCronExpression(updated.getCronExpression());
        existing.setNextRunAt(calculateNextRunAt(updated.getCronExpression()));
        log.info("Updated scheduled trigger: {}", id);
        return triggerRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        ScheduledTriggerEntity existing = findById(id);
        triggerRepository.delete(existing);
        log.info("Deleted scheduled trigger: {}", id);
    }

    @Transactional
    public ScheduledTriggerEntity activate(UUID id) {
        ScheduledTriggerEntity trigger = findById(id);
        trigger.setActive(true);
        trigger.setNextRunAt(calculateNextRunAt(trigger.getCronExpression()));
        log.info("Activated scheduled trigger: {}", id);
        return triggerRepository.save(trigger);
    }

    @Transactional
    public ScheduledTriggerEntity deactivate(UUID id) {
        ScheduledTriggerEntity trigger = findById(id);
        trigger.setActive(false);
        log.info("Deactivated scheduled trigger: {}", id);
        return triggerRepository.save(trigger);
    }

    /**
     * Returns all triggers whose nextRunAt is before now and that are active.
     */
    @Transactional(readOnly = true)
    public List<ScheduledTriggerEntity> getTriggersToRun() {
        return triggerRepository.findByNextRunAtBeforeAndActiveTrue(Instant.now());
    }

    /**
     * Updates the trigger after a run: sets lastRunAt to now and calculates the next run time.
     */
    @Transactional
    public ScheduledTriggerEntity updateAfterRun(UUID triggerId) {
        ScheduledTriggerEntity trigger = findById(triggerId);
        trigger.setLastRunAt(Instant.now());
        trigger.setNextRunAt(calculateNextRunAt(trigger.getCronExpression()));
        log.info("Updated trigger {} after run, next run at {}", triggerId, trigger.getNextRunAt());
        return triggerRepository.save(trigger);
    }

    /**
     * Parses the cron expression using Spring's CronExpression and calculates the next run time.
     */
    private Instant calculateNextRunAt(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            LocalDateTime next = cron.next(LocalDateTime.now());
            if (next != null) {
                return next.atZone(ZoneId.of("Europe/Berlin")).toInstant();
            }
        } catch (Exception e) {
            log.warn("Failed to parse cron expression '{}': {}", cronExpression, e.getMessage());
        }
        // Fallback: 1 hour from now
        return Instant.now().plus(Duration.ofHours(1));
    }
}
