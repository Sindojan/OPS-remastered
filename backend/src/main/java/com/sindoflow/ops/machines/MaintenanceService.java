package com.sindoflow.ops.machines;

import com.sindoflow.ops.machines.dto.CreateMaintenanceIntervalRequest;
import com.sindoflow.ops.machines.dto.MaintenanceIntervalResponse;
import com.sindoflow.ops.machines.dto.MaintenanceRecordResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    private final MaintenanceIntervalRepository intervalRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final MachineRepository machineRepository;

    public MaintenanceService(MaintenanceIntervalRepository intervalRepository,
                              MaintenanceRecordRepository recordRepository,
                              MachineRepository machineRepository) {
        this.intervalRepository = intervalRepository;
        this.recordRepository = recordRepository;
        this.machineRepository = machineRepository;
    }

    @Transactional
    public MaintenanceIntervalResponse createInterval(CreateMaintenanceIntervalRequest request) {
        if (!machineRepository.existsById(request.machineId())) {
            throw new EntityNotFoundException("Machine not found: " + request.machineId());
        }

        MaintenanceIntervalEntity entity = new MaintenanceIntervalEntity();
        entity.setMachineId(request.machineId());
        entity.setType(request.type());
        entity.setIntervalDays(request.intervalDays());
        entity.setIntervalHours(request.intervalHours());
        entity.setNextDueAt(request.nextDueAt());
        entity.setDescription(request.description());
        entity = intervalRepository.save(entity);
        log.info("Created maintenance interval: {} for machine {}", entity.getId(), request.machineId());
        return toIntervalResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceIntervalResponse> getIntervalsByMachine(UUID machineId) {
        return intervalRepository.findByMachineId(machineId).stream()
                .map(this::toIntervalResponse)
                .toList();
    }

    @Transactional
    public MaintenanceRecordResponse performMaintenance(UUID machineId, UUID intervalId,
                                                         UUID performedBy, int durationMinutes, String notes) {
        if (!machineRepository.existsById(machineId)) {
            throw new EntityNotFoundException("Machine not found: " + machineId);
        }

        MaintenanceRecordEntity record = new MaintenanceRecordEntity();
        record.setMachineId(machineId);
        record.setIntervalId(intervalId);
        record.setPerformedBy(performedBy);
        record.setPerformedAt(Instant.now());
        record.setDurationMinutes(durationMinutes);
        record.setNotes(notes);
        record.setStatus(MaintenanceRecordStatus.DONE);
        record = recordRepository.save(record);

        // Update interval if provided
        if (intervalId != null) {
            MaintenanceIntervalEntity interval = intervalRepository.findById(intervalId)
                    .orElseThrow(() -> new EntityNotFoundException("Maintenance interval not found: " + intervalId));

            Instant now = Instant.now();
            interval.setLastPerformedAt(now);

            // Calculate next due date based on interval type
            if (interval.getType() == MaintenanceType.TIME_BASED && interval.getIntervalDays() != null) {
                interval.setNextDueAt(now.plus(Duration.ofDays(interval.getIntervalDays())));
            } else if (interval.getType() == MaintenanceType.HOURS_BASED && interval.getIntervalHours() != null) {
                interval.setNextDueAt(now.plus(Duration.ofHours(interval.getIntervalHours())));
            }

            intervalRepository.save(interval);
            log.info("Updated maintenance interval {} after maintenance", intervalId);
        }

        log.info("Recorded maintenance for machine {}", machineId);
        return toRecordResponse(record);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceIntervalResponse> getOverdue() {
        return intervalRepository.findByNextDueAtBefore(Instant.now()).stream()
                .map(this::toIntervalResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceIntervalResponse> getUpcoming(int days) {
        Instant now = Instant.now();
        Instant until = now.plus(Duration.ofDays(days));
        return intervalRepository.findByNextDueAtBetween(now, until).stream()
                .map(this::toIntervalResponse)
                .toList();
    }

    private MaintenanceIntervalResponse toIntervalResponse(MaintenanceIntervalEntity entity) {
        return new MaintenanceIntervalResponse(
                entity.getId(),
                entity.getMachineId(),
                entity.getType(),
                entity.getIntervalDays(),
                entity.getIntervalHours(),
                entity.getLastPerformedAt(),
                entity.getNextDueAt(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private MaintenanceRecordResponse toRecordResponse(MaintenanceRecordEntity entity) {
        return new MaintenanceRecordResponse(
                entity.getId(),
                entity.getMachineId(),
                entity.getIntervalId(),
                entity.getPerformedBy(),
                entity.getPerformedAt(),
                entity.getDurationMinutes(),
                entity.getNotes(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
