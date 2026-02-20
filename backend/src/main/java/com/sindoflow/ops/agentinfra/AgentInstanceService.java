package com.sindoflow.ops.agentinfra;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentInstanceService {

    private static final Logger log = LoggerFactory.getLogger(AgentInstanceService.class);

    private final AgentInstanceRepository instanceRepository;
    private final AgentTemplateRepository templateRepository;

    public AgentInstanceService(AgentInstanceRepository instanceRepository,
                                AgentTemplateRepository templateRepository) {
        this.instanceRepository = instanceRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public List<AgentInstanceEntity> findAll() {
        return instanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AgentInstanceEntity findById(UUID id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AgentInstance not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AgentInstanceEntity> findByTemplateId(UUID templateId) {
        return instanceRepository.findByTemplateId(templateId);
    }

    @Transactional(readOnly = true)
    public List<AgentInstanceEntity> findByStatus(AgentInstanceStatus status) {
        return instanceRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<AgentInstanceEntity> findByTenantId(String tenantId) {
        return instanceRepository.findByTenantId(tenantId);
    }

    @Transactional
    public AgentInstanceEntity create(AgentInstanceEntity instance) {
        // Verify template exists
        templateRepository.findById(instance.getTemplateId())
                .orElseThrow(() -> new EntityNotFoundException("AgentTemplate not found: " + instance.getTemplateId()));

        instance.setStatus(AgentInstanceStatus.INACTIVE);
        log.info("Creating agent instance: {} (type={})", instance.getName(), instance.getType());
        return instanceRepository.save(instance);
    }

    @Transactional
    public AgentInstanceEntity activate(UUID id) {
        AgentInstanceEntity instance = findById(id);
        validateTransition(instance.getStatus(), AgentInstanceStatus.ACTIVE);
        instance.setStatus(AgentInstanceStatus.ACTIVE);
        log.info("Activated agent instance: {}", id);
        return instanceRepository.save(instance);
    }

    @Transactional
    public AgentInstanceEntity deactivate(UUID id) {
        AgentInstanceEntity instance = findById(id);
        validateTransition(instance.getStatus(), AgentInstanceStatus.INACTIVE);
        instance.setStatus(AgentInstanceStatus.INACTIVE);
        log.info("Deactivated agent instance: {}", id);
        return instanceRepository.save(instance);
    }

    @Transactional
    public AgentInstanceEntity quarantine(UUID id) {
        AgentInstanceEntity instance = findById(id);
        validateTransition(instance.getStatus(), AgentInstanceStatus.QUARANTINE);
        instance.setStatus(AgentInstanceStatus.QUARANTINE);
        log.info("Quarantined agent instance: {}", id);
        return instanceRepository.save(instance);
    }

    @Transactional
    public AgentInstanceEntity terminate(UUID id) {
        AgentInstanceEntity instance = findById(id);
        validateTransition(instance.getStatus(), AgentInstanceStatus.TERMINATED);
        instance.setStatus(AgentInstanceStatus.TERMINATED);
        instance.setTerminatedAt(Instant.now());
        log.info("Terminated agent instance: {}", id);
        return instanceRepository.save(instance);
    }

    @Transactional
    public void delete(UUID id) {
        AgentInstanceEntity existing = findById(id);
        instanceRepository.delete(existing);
        log.info("Deleted agent instance: {}", id);
    }

    /**
     * Validates state transitions:
     * INACTIVE -> ACTIVE
     * ACTIVE -> INACTIVE, QUARANTINE, TERMINATED
     * QUARANTINE -> ACTIVE, TERMINATED
     * TERMINATED -> (terminal, no transitions)
     */
    private void validateTransition(AgentInstanceStatus from, AgentInstanceStatus to) {
        boolean valid = switch (from) {
            case INACTIVE -> to == AgentInstanceStatus.ACTIVE;
            case ACTIVE -> to == AgentInstanceStatus.INACTIVE
                    || to == AgentInstanceStatus.QUARANTINE
                    || to == AgentInstanceStatus.TERMINATED;
            case QUARANTINE -> to == AgentInstanceStatus.ACTIVE
                    || to == AgentInstanceStatus.TERMINATED;
            case TERMINATED -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid status transition: " + from + " -> " + to);
        }
    }
}
