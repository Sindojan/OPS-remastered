package com.owlsburg.ops.systemagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SystemAgentIncidentService {

    private static final Logger log = LoggerFactory.getLogger(SystemAgentIncidentService.class);

    private final SystemAgentIncidentRepository incidentRepository;

    public SystemAgentIncidentService(SystemAgentIncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public SystemAgentIncidentEntity report(UUID instanceId, String type, String description) {
        SystemAgentIncidentEntity incident = new SystemAgentIncidentEntity();
        incident.setInstanceId(instanceId);
        incident.setType(type);
        incident.setDescription(description);

        log.warn("System agent incident reported: instance={}, type={}, desc={}",
                instanceId, type, description);
        return incidentRepository.save(incident);
    }

    @Transactional
    public void resolve(UUID incidentId) {
        incidentRepository.findById(incidentId).ifPresent(incident -> {
            incident.setResolvedAt(Instant.now());
            incidentRepository.save(incident);
            log.info("Resolved system agent incident: {}", incidentId);
        });
    }

    @Transactional(readOnly = true)
    public List<SystemAgentIncidentEntity> findByInstance(UUID instanceId) {
        return incidentRepository.findByInstanceIdOrderByCreatedAtDesc(instanceId);
    }

    @Transactional(readOnly = true)
    public List<SystemAgentIncidentEntity> findUnresolved(UUID instanceId) {
        return incidentRepository.findByInstanceIdAndResolvedAtIsNull(instanceId);
    }

    @Transactional(readOnly = true)
    public long countOpenIncidents() {
        return incidentRepository.countByResolvedAtIsNull();
    }
}
