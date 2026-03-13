package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.agentinfra.AgentActivityStatus;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SystemAgentInstanceService {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentInstanceService.class);

    private final SystemAgentInstanceRepository instanceRepository;

    public SystemAgentInstanceService(SystemAgentInstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    @Transactional(readOnly = true)
    public List<SystemAgentInstanceEntity> findAll() {
        return instanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SystemAgentInstanceEntity findById(UUID id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("System-Agent-Instanz nicht gefunden: " + id));
    }

    @Transactional
    public void updateActivityStatus(UUID instanceId, AgentActivityStatus activityStatus) {
        SystemAgentInstanceEntity instance = findById(instanceId);
        instance.setActivityStatus(activityStatus);
        instance.setActivityStatusChangedAt(Instant.now());
        instanceRepository.save(instance);
        log.debug("Updated system agent activity status for {} to {}", instanceId, activityStatus);
    }

    @Transactional
    public void linkLastRun(UUID instanceId, UUID runId) {
        SystemAgentInstanceEntity instance = findById(instanceId);
        instance.setLastRunId(runId);
        instanceRepository.save(instance);
    }

    @Transactional
    public SystemAgentInstanceEntity save(SystemAgentInstanceEntity instance) {
        return instanceRepository.save(instance);
    }
}
