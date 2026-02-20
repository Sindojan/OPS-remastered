package com.sindoflow.ops.production;

import com.sindoflow.ops.production.dto.CreateShiftRequest;
import com.sindoflow.ops.production.dto.ShiftResponse;
import com.sindoflow.ops.production.dto.UpdateShiftRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ShiftService {

    private static final Logger log = LoggerFactory.getLogger(ShiftService.class);

    private final ShiftRepository shiftRepository;

    public ShiftService(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    @Transactional
    public ShiftResponse create(CreateShiftRequest request) {
        ShiftEntity entity = new ShiftEntity();
        entity.setName(request.name());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setDaysOfWeek(request.daysOfWeek());
        entity.setCapacityHours(request.capacityHours());
        entity = shiftRepository.save(entity);
        log.info("Created shift: {}", entity.getId());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public ShiftResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> getAll() {
        return shiftRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShiftResponse update(UUID id, UpdateShiftRequest request) {
        ShiftEntity entity = findOrThrow(id);
        if (request.name() != null) entity.setName(request.name());
        if (request.startTime() != null) entity.setStartTime(request.startTime());
        if (request.endTime() != null) entity.setEndTime(request.endTime());
        if (request.daysOfWeek() != null) entity.setDaysOfWeek(request.daysOfWeek());
        if (request.capacityHours() != null) entity.setCapacityHours(request.capacityHours());
        entity = shiftRepository.save(entity);
        log.info("Updated shift: {}", entity.getId());
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!shiftRepository.existsById(id)) {
            throw new EntityNotFoundException("Shift not found: " + id);
        }
        shiftRepository.deleteById(id);
        log.info("Deleted shift: {}", id);
    }

    private ShiftEntity findOrThrow(UUID id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found: " + id));
    }

    private ShiftResponse toResponse(ShiftEntity entity) {
        return new ShiftResponse(
                entity.getId(),
                entity.getName(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDaysOfWeek(),
                entity.getCapacityHours(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
