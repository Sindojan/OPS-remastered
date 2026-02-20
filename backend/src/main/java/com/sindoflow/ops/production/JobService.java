package com.sindoflow.ops.production;

import com.sindoflow.ops.production.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private static final Map<JobStatus, Set<JobStatus>> VALID_TRANSITIONS = Map.of(
            JobStatus.DRAFT, Set.of(JobStatus.RELEASED, JobStatus.CANCELLED),
            JobStatus.RELEASED, Set.of(JobStatus.IN_PRODUCTION, JobStatus.ON_HOLD, JobStatus.CANCELLED),
            JobStatus.IN_PRODUCTION, Set.of(JobStatus.ON_HOLD, JobStatus.COMPLETED, JobStatus.CANCELLED),
            JobStatus.ON_HOLD, Set.of(JobStatus.RELEASED, JobStatus.IN_PRODUCTION, JobStatus.CANCELLED)
    );

    private final JobRepository jobRepository;
    private final JobStatusHistoryRepository statusHistoryRepository;
    private final StationRepository stationRepository;
    private final StationShiftRepository stationShiftRepository;

    public JobService(JobRepository jobRepository,
                      JobStatusHistoryRepository statusHistoryRepository,
                      StationRepository stationRepository,
                      StationShiftRepository stationShiftRepository) {
        this.jobRepository = jobRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.stationRepository = stationRepository;
        this.stationShiftRepository = stationShiftRepository;
    }

    @Transactional
    public JobResponse create(CreateJobRequest request) {
        JobEntity entity = new JobEntity();
        entity.setJobNumber(request.jobNumber());
        entity.setCustomerId(request.customerId());
        entity.setTitle(request.title());
        entity.setStatus(JobStatus.DRAFT);
        entity.setPriority(request.priority());
        entity.setQuantity(request.quantity());
        entity.setDeadline(request.deadline());
        entity.setNotes(request.notes());
        entity.setCreatedBy(request.createdBy());
        entity = jobRepository.save(entity);

        // Record initial status
        recordStatusChange(entity.getId(), null, JobStatus.DRAFT, request.createdBy(), "Job created");

        log.info("Created job: {} ({})", entity.getId(), entity.getJobNumber());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public JobResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getAll(Pageable pageable) {
        return jobRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public JobResponse update(UUID id, UpdateJobRequest request) {
        JobEntity entity = findOrThrow(id);
        if (request.customerId() != null) entity.setCustomerId(request.customerId());
        if (request.title() != null) entity.setTitle(request.title());
        if (request.priority() != null) entity.setPriority(request.priority());
        if (request.quantity() != null) entity.setQuantity(request.quantity());
        if (request.deadline() != null) entity.setDeadline(request.deadline());
        if (request.notes() != null) entity.setNotes(request.notes());
        entity = jobRepository.save(entity);
        log.info("Updated job: {}", entity.getId());
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!jobRepository.existsById(id)) {
            throw new EntityNotFoundException("Job not found: " + id);
        }
        jobRepository.deleteById(id);
        log.info("Deleted job: {}", id);
    }

    @Transactional
    public JobResponse changeStatus(UUID jobId, JobStatus newStatus, UUID changedBy, String reason) {
        JobEntity entity = findOrThrow(jobId);
        JobStatus currentStatus = entity.getStatus();

        Set<JobStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        entity.setStatus(newStatus);

        if (newStatus == JobStatus.IN_PRODUCTION && entity.getStartedAt() == null) {
            entity.setStartedAt(Instant.now());
        }
        if (newStatus == JobStatus.COMPLETED) {
            entity.setCompletedAt(Instant.now());
        }

        entity = jobRepository.save(entity);
        recordStatusChange(jobId, currentStatus, newStatus, changedBy, reason);

        log.info("Job {} status changed: {} -> {}", jobId, currentStatus, newStatus);
        return toResponse(entity);
    }

    @Transactional
    public JobResponse assignToStation(UUID jobId, UUID stationId, UUID shiftId) {
        JobEntity entity = findOrThrow(jobId);

        if (!stationRepository.existsById(stationId)) {
            throw new EntityNotFoundException("Station not found: " + stationId);
        }

        StationShiftId ssId = new StationShiftId(stationId, shiftId);
        if (!stationShiftRepository.existsById(ssId)) {
            throw new IllegalArgumentException("Shift is not assigned to the specified station");
        }

        // Check station capacity: count active jobs at that station+shift
        long activeJobs = jobRepository.countByAssignedStationIdAndShiftIdAndStatusIn(
                stationId, shiftId,
                List.of(JobStatus.RELEASED, JobStatus.IN_PRODUCTION, JobStatus.ON_HOLD)
        );

        StationEntity station = stationRepository.findById(stationId).orElseThrow();
        if (station.getCapacityPerShift() != null && activeJobs >= station.getCapacityPerShift()) {
            throw new IllegalArgumentException("Station capacity exceeded for this shift");
        }

        entity.setAssignedStationId(stationId);
        entity.setShiftId(shiftId);
        entity = jobRepository.save(entity);
        log.info("Job {} assigned to station {} shift {}", jobId, stationId, shiftId);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> findByStatus(JobStatus status, Pageable pageable) {
        return jobRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> findByCustomer(UUID customerId, Pageable pageable) {
        return jobRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    private void recordStatusChange(UUID jobId, JobStatus from, JobStatus to, UUID changedBy, String reason) {
        JobStatusHistoryEntity history = new JobStatusHistoryEntity();
        history.setJobId(jobId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(changedBy);
        history.setChangedAt(Instant.now());
        history.setReason(reason);
        statusHistoryRepository.save(history);
    }

    private JobEntity findOrThrow(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + id));
    }

    private JobResponse toResponse(JobEntity entity) {
        List<JobStatusHistoryResponse> history = statusHistoryRepository
                .findByJobIdOrderByChangedAtAsc(entity.getId())
                .stream()
                .map(h -> new JobStatusHistoryResponse(
                        h.getId(), h.getFromStatus(), h.getToStatus(),
                        h.getChangedBy(), h.getChangedAt(), h.getReason()))
                .toList();

        return new JobResponse(
                entity.getId(),
                entity.getJobNumber(),
                entity.getCustomerId(),
                entity.getTitle(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getQuantity(),
                entity.getDeadline(),
                entity.getNotes(),
                entity.getCreatedBy(),
                entity.getAssignedStationId(),
                entity.getShiftId(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                history,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
