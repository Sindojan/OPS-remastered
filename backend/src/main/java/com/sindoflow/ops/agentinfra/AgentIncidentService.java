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
public class AgentIncidentService {

    private static final Logger log = LoggerFactory.getLogger(AgentIncidentService.class);

    private final AgentIncidentRepository incidentRepository;
    private final AgentInstanceRepository instanceRepository;

    public AgentIncidentService(AgentIncidentRepository incidentRepository,
                                AgentInstanceRepository instanceRepository) {
        this.incidentRepository = incidentRepository;
        this.instanceRepository = instanceRepository;
    }

    @Transactional
    public AgentIncidentEntity report(UUID instanceId, String type, String description) {
        // Verify instance exists
        instanceRepository.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("AgentInstance not found: " + instanceId));

        AgentIncidentEntity incident = new AgentIncidentEntity();
        incident.setInstanceId(instanceId);
        incident.setType(type);
        incident.setDescription(description);

        log.info("Reporting agent incident for instance {} (type={})", instanceId, type);
        return incidentRepository.save(incident);
    }

    @Transactional
    public AgentIncidentEntity resolve(UUID incidentId) {
        AgentIncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("AgentIncident not found: " + incidentId));

        incident.setResolvedAt(Instant.now());
        log.info("Resolved agent incident: {}", incidentId);
        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public List<AgentIncidentEntity> findUnresolved() {
        return incidentRepository.findByResolvedAtIsNull();
    }

    @Transactional(readOnly = true)
    public List<AgentIncidentEntity> findByInstanceId(UUID instanceId) {
        return incidentRepository.findByInstanceId(instanceId);
    }

    @Transactional(readOnly = true)
    public AgentIncidentEntity findById(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AgentIncident not found: " + id));
    }
}
