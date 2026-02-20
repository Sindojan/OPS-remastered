package com.sindoflow.ops.machines;

import com.sindoflow.ops.machines.dto.CreateMachineRequest;
import com.sindoflow.ops.machines.dto.MachineResponse;
import com.sindoflow.ops.machines.dto.UpdateMachineRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MachineService {

    private static final Logger log = LoggerFactory.getLogger(MachineService.class);

    private static final Map<MachineStatus, Set<MachineStatus>> VALID_TRANSITIONS = Map.of(
            MachineStatus.AVAILABLE, Set.of(MachineStatus.IN_USE, MachineStatus.MAINTENANCE, MachineStatus.BLOCKED, MachineStatus.DECOMMISSIONED),
            MachineStatus.IN_USE, Set.of(MachineStatus.AVAILABLE, MachineStatus.MAINTENANCE, MachineStatus.BLOCKED),
            MachineStatus.MAINTENANCE, Set.of(MachineStatus.AVAILABLE, MachineStatus.BLOCKED, MachineStatus.DECOMMISSIONED),
            MachineStatus.BLOCKED, Set.of(MachineStatus.AVAILABLE, MachineStatus.MAINTENANCE, MachineStatus.DECOMMISSIONED)
    );

    private final MachineRepository machineRepository;

    public MachineService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    @Transactional
    public MachineResponse create(CreateMachineRequest request) {
        MachineEntity entity = new MachineEntity();
        entity.setName(request.name());
        entity.setMachineNumber(request.machineNumber());
        entity.setType(request.type());
        entity.setStationId(request.stationId());
        entity.setStatus(MachineStatus.AVAILABLE);
        entity.setCapacityPerHour(request.capacityPerHour());
        entity.setManufacturer(request.manufacturer());
        entity.setModel(request.model());
        entity.setSerialNumber(request.serialNumber());
        entity.setPurchaseDate(request.purchaseDate());
        entity = machineRepository.save(entity);
        log.info("Created machine: {} ({})", entity.getId(), entity.getMachineNumber());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public MachineResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> getAll() {
        return machineRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public MachineResponse update(UUID id, UpdateMachineRequest request) {
        MachineEntity entity = findOrThrow(id);
        if (request.name() != null) entity.setName(request.name());
        if (request.type() != null) entity.setType(request.type());
        if (request.stationId() != null) entity.setStationId(request.stationId());
        if (request.capacityPerHour() != null) entity.setCapacityPerHour(request.capacityPerHour());
        if (request.manufacturer() != null) entity.setManufacturer(request.manufacturer());
        if (request.model() != null) entity.setModel(request.model());
        if (request.serialNumber() != null) entity.setSerialNumber(request.serialNumber());
        if (request.purchaseDate() != null) entity.setPurchaseDate(request.purchaseDate());
        entity = machineRepository.save(entity);
        log.info("Updated machine: {}", entity.getId());
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!machineRepository.existsById(id)) {
            throw new EntityNotFoundException("Machine not found: " + id);
        }
        machineRepository.deleteById(id);
        log.info("Deleted machine: {}", id);
    }

    @Transactional
    public MachineResponse changeStatus(UUID id, MachineStatus newStatus) {
        MachineEntity entity = findOrThrow(id);
        MachineStatus currentStatus = entity.getStatus();

        Set<MachineStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        entity.setStatus(newStatus);
        entity = machineRepository.save(entity);
        log.info("Machine {} status changed: {} -> {}", id, currentStatus, newStatus);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> getByStation(UUID stationId) {
        return machineRepository.findByStationId(stationId).stream().map(this::toResponse).toList();
    }

    private MachineEntity findOrThrow(UUID id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + id));
    }

    private MachineResponse toResponse(MachineEntity entity) {
        return new MachineResponse(
                entity.getId(),
                entity.getName(),
                entity.getMachineNumber(),
                entity.getType(),
                entity.getStationId(),
                entity.getStatus(),
                entity.getCapacityPerHour(),
                entity.getManufacturer(),
                entity.getModel(),
                entity.getSerialNumber(),
                entity.getPurchaseDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
