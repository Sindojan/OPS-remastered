package com.sindoflow.ops.events;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DomainEventService {

    private static final Logger log = LoggerFactory.getLogger(DomainEventService.class);

    private final DomainEventRepository eventRepository;

    public DomainEventService(DomainEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public DomainEventEntity publish(String eventType, String sourceType, UUID sourceId, String payload) {
        DomainEventEntity event = new DomainEventEntity();
        event.setEventType(eventType);
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        event.setPayload(payload != null ? payload : "{}");
        event.setProcessed(false);

        log.info("Publishing domain event: {} from {}/{}", eventType, sourceType, sourceId);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEntity> getUnprocessed() {
        return eventRepository.findByProcessedFalse();
    }

    @Transactional
    public DomainEventEntity markProcessed(UUID eventId) {
        DomainEventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("DomainEvent not found: " + eventId));
        event.setProcessed(true);
        log.info("Marked domain event as processed: {}", eventId);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEntity> findBySource(String sourceType, UUID sourceId) {
        return eventRepository.findBySourceTypeAndSourceId(sourceType, sourceId);
    }

    @Transactional(readOnly = true)
    public DomainEventEntity findById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DomainEvent not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<DomainEventEntity> findAll() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEntity> findByEventType(String eventType) {
        return eventRepository.findByEventType(eventType);
    }
}
