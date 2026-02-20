package com.sindoflow.ops.people;

import com.sindoflow.ops.people.dto.MyDayResponse;
import com.sindoflow.ops.people.dto.TimeEntryResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TimeTrackingService {

    private static final Logger log = LoggerFactory.getLogger(TimeTrackingService.class);

    private final TimeEntryRepository timeEntryRepository;
    private final TimeCorrectionRepository timeCorrectionRepository;
    private final EmployeeRepository employeeRepository;

    public TimeTrackingService(TimeEntryRepository timeEntryRepository,
                               TimeCorrectionRepository timeCorrectionRepository,
                               EmployeeRepository employeeRepository) {
        this.timeEntryRepository = timeEntryRepository;
        this.timeCorrectionRepository = timeCorrectionRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public TimeEntryEntity clockIn(UUID employeeId) {
        validateEmployeeExists(employeeId);

        Optional<TimeEntryEntity> last = timeEntryRepository.findLastByEmployeeId(employeeId);
        if (last.isPresent() && last.get().getType() == TimeEntryType.CLOCK_IN) {
            throw new IllegalArgumentException("Employee already clocked in. Must clock out first.");
        }

        TimeEntryEntity entry = new TimeEntryEntity();
        entry.setEmployeeId(employeeId);
        entry.setType(TimeEntryType.CLOCK_IN);
        entry.setTimestamp(Instant.now());
        return timeEntryRepository.save(entry);
    }

    @Transactional
    public TimeEntryEntity clockOut(UUID employeeId) {
        validateEmployeeExists(employeeId);

        Optional<TimeEntryEntity> last = timeEntryRepository.findLastByEmployeeId(employeeId);
        if (last.isEmpty() || last.get().getType() != TimeEntryType.CLOCK_IN) {
            throw new IllegalArgumentException("Employee is not clocked in. Must clock in first.");
        }

        TimeEntryEntity entry = new TimeEntryEntity();
        entry.setEmployeeId(employeeId);
        entry.setType(TimeEntryType.CLOCK_OUT);
        entry.setTimestamp(Instant.now());
        return timeEntryRepository.save(entry);
    }

    @Transactional
    public TimeEntryEntity startJob(UUID employeeId, UUID jobId) {
        validateEmployeeExists(employeeId);

        TimeEntryEntity entry = new TimeEntryEntity();
        entry.setEmployeeId(employeeId);
        entry.setType(TimeEntryType.JOB_START);
        entry.setJobId(jobId);
        entry.setTimestamp(Instant.now());
        return timeEntryRepository.save(entry);
    }

    @Transactional
    public TimeEntryEntity endJob(UUID employeeId, UUID jobId) {
        validateEmployeeExists(employeeId);

        TimeEntryEntity entry = new TimeEntryEntity();
        entry.setEmployeeId(employeeId);
        entry.setType(TimeEntryType.JOB_END);
        entry.setJobId(jobId);
        entry.setTimestamp(Instant.now());
        return timeEntryRepository.save(entry);
    }

    @Transactional
    public TimeCorrectionEntity correctEntry(UUID timeEntryId, Instant newTimestamp,
                                             UUID correctedBy, String reason) {
        TimeEntryEntity entry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new EntityNotFoundException("Time entry not found: " + timeEntryId));

        TimeCorrectionEntity correction = new TimeCorrectionEntity();
        correction.setTimeEntryId(timeEntryId);
        correction.setOldTimestamp(entry.getTimestamp());
        correction.setNewTimestamp(newTimestamp);
        correction.setCorrectedBy(correctedBy);
        correction.setReason(reason);

        entry.setTimestamp(newTimestamp);
        timeEntryRepository.save(entry);

        return timeCorrectionRepository.save(correction);
    }

    @Transactional(readOnly = true)
    public Page<TimeEntryEntity> getEntries(UUID employeeId, Instant from, Instant to, Pageable pageable) {
        return timeEntryRepository.findByEmployeeIdAndTimestampBetween(employeeId, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public MyDayResponse getMyDay(UUID employeeId) {
        validateEmployeeExists(employeeId);

        LocalDate today = LocalDate.now();
        Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<TimeEntryEntity> entries = timeEntryRepository
                .findByEmployeeIdAndTimestampBetweenOrderByTimestampAsc(employeeId, dayStart, dayEnd);

        boolean clockedIn = false;
        for (TimeEntryEntity entry : entries) {
            if (entry.getType() == TimeEntryType.CLOCK_IN) {
                clockedIn = true;
            } else if (entry.getType() == TimeEntryType.CLOCK_OUT) {
                clockedIn = false;
            }
        }

        List<TimeEntryResponse> entryResponses = entries.stream()
                .map(TimeEntryResponse::from)
                .toList();

        return new MyDayResponse(employeeId, clockedIn, entryResponses);
    }

    private void validateEmployeeExists(UUID employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EntityNotFoundException("Employee not found: " + employeeId);
        }
    }
}
