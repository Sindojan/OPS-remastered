package com.sindoflow.ops.machines;

import com.sindoflow.ops.machines.dto.MachineIncidentResponse;
import com.sindoflow.ops.machines.dto.ReportIncidentRequest;
import com.sindoflow.ops.machines.dto.ResolveIncidentRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MachineIncidentService {

    private static final Logger log = LoggerFactory.getLogger(MachineIncidentService.class);

    private final MachineIncidentRepository incidentRepository;
    private final MachineRepository machineRepository;

    public MachineIncidentService(MachineIncidentRepository incidentRepository,
                                   MachineRepository machineRepository) {
        this.incidentRepository = incidentRepository;
        this.machineRepository = machineRepository;
    }

    @Transactional
    public MachineIncidentResponse report(UUID machineId, ReportIncidentRequest request) {
        if (!machineRepository.existsById(machineId)) {
            throw new EntityNotFoundException("Machine not found: " + machineId);
        }

        MachineIncidentEntity entity = new MachineIncidentEntity();
        entity.setMachineId(machineId);
        entity.setReportedBy(request.reportedBy());
        entity.setType(request.type());
        entity.setDescription(request.description());
        entity.setSeverity(request.severity());
        entity.setReportedAt(Instant.now());
        entity = incidentRepository.save(entity);
        log.info("Reported incident {} for machine {}", entity.getId(), machineId);
        return toResponse(entity);
    }

    @Transactional
    public MachineIncidentResponse resolve(UUID incidentId, ResolveIncidentRequest request) {
        MachineIncidentEntity entity = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Incident not found: " + incidentId));

        if (entity.getResolvedAt() != null) {
            throw new IllegalArgumentException("Incident already resolved");
        }

        entity.setResolvedAt(Instant.now());
        entity.setResolutionNotes(request.resolutionNotes());
        entity = incidentRepository.save(entity);
        log.info("Resolved incident {}", incidentId);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<MachineIncidentResponse> findByMachine(UUID machineId) {
        return incidentRepository.findByMachineId(machineId).stream()
                .map(this::toResponse)
                .toList();
    }

    private MachineIncidentResponse toResponse(MachineIncidentEntity entity) {
        return new MachineIncidentResponse(
                entity.getId(),
                entity.getMachineId(),
                entity.getReportedBy(),
                entity.getType(),
                entity.getDescription(),
                entity.getSeverity(),
                entity.getReportedAt(),
                entity.getResolvedAt(),
                entity.getResolutionNotes()
        );
    }
}
