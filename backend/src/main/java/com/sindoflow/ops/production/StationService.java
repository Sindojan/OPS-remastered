package com.sindoflow.ops.production;

import com.sindoflow.ops.production.dto.CreateStationRequest;
import com.sindoflow.ops.production.dto.StationResponse;
import com.sindoflow.ops.production.dto.UpdateStationRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StationService {

    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    private final StationRepository stationRepository;
    private final ShiftRepository shiftRepository;
    private final StationShiftRepository stationShiftRepository;

    public StationService(StationRepository stationRepository,
                          ShiftRepository shiftRepository,
                          StationShiftRepository stationShiftRepository) {
        this.stationRepository = stationRepository;
        this.shiftRepository = shiftRepository;
        this.stationShiftRepository = stationShiftRepository;
    }

    @Transactional
    public StationResponse create(CreateStationRequest request) {
        StationEntity entity = new StationEntity();
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setCapacityPerShift(request.capacityPerShift());
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        entity = stationRepository.save(entity);
        log.info("Created station: {}", entity.getId());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public StationResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<StationResponse> getAll() {
        return stationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public StationResponse update(UUID id, UpdateStationRequest request) {
        StationEntity entity = findOrThrow(id);
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.capacityPerShift() != null) entity.setCapacityPerShift(request.capacityPerShift());
        if (request.status() != null) entity.setStatus(request.status());
        entity = stationRepository.save(entity);
        log.info("Updated station: {}", entity.getId());
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!stationRepository.existsById(id)) {
            throw new EntityNotFoundException("Station not found: " + id);
        }
        stationRepository.deleteById(id);
        log.info("Deleted station: {}", id);
    }

    @Transactional
    public StationResponse addShift(UUID stationId, UUID shiftId) {
        StationEntity station = findOrThrow(stationId);
        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found: " + shiftId));

        StationShiftId compositeId = new StationShiftId(stationId, shiftId);
        if (stationShiftRepository.existsById(compositeId)) {
            throw new IllegalArgumentException("Shift already assigned to station");
        }

        StationShiftEntity stationShift = new StationShiftEntity(station, shift);
        stationShiftRepository.save(stationShift);
        log.info("Added shift {} to station {}", shiftId, stationId);

        // Refresh to get updated list
        station = findOrThrow(stationId);
        return toResponse(station);
    }

    @Transactional
    public StationResponse removeShift(UUID stationId, UUID shiftId) {
        if (!stationRepository.existsById(stationId)) {
            throw new EntityNotFoundException("Station not found: " + stationId);
        }

        StationShiftId compositeId = new StationShiftId(stationId, shiftId);
        if (!stationShiftRepository.existsById(compositeId)) {
            throw new EntityNotFoundException("Shift not assigned to station");
        }

        stationShiftRepository.deleteById(compositeId);
        log.info("Removed shift {} from station {}", shiftId, stationId);

        StationEntity station = findOrThrow(stationId);
        return toResponse(station);
    }

    private StationEntity findOrThrow(UUID id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + id));
    }

    private StationResponse toResponse(StationEntity entity) {
        int shiftCount = entity.getStationShifts() != null ? entity.getStationShifts().size() : 0;
        int capacityPerShift = entity.getCapacityPerShift() != null ? entity.getCapacityPerShift() : 0;
        int totalCapacity = capacityPerShift * shiftCount;

        List<UUID> shiftIds = entity.getStationShifts() != null
                ? entity.getStationShifts().stream()
                    .map(ss -> ss.getId().getShiftId())
                    .toList()
                : List.of();

        return new StationResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCapacityPerShift(),
                entity.getStatus(),
                totalCapacity,
                shiftIds,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
