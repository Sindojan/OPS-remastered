package com.sindoflow.ops.people;

import com.sindoflow.ops.people.dto.CreateAbsenceRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AbsenceService {

    private static final Logger log = LoggerFactory.getLogger(AbsenceService.class);

    private final AbsenceRepository absenceRepository;
    private final EmployeeRepository employeeRepository;

    public AbsenceService(AbsenceRepository absenceRepository,
                          EmployeeRepository employeeRepository) {
        this.absenceRepository = absenceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public AbsenceEntity request(CreateAbsenceRequest request) {
        if (!employeeRepository.existsById(request.employeeId())) {
            throw new EntityNotFoundException("Employee not found: " + request.employeeId());
        }
        if (request.toDate().isBefore(request.fromDate())) {
            throw new IllegalArgumentException("toDate must not be before fromDate");
        }

        AbsenceEntity entity = new AbsenceEntity();
        entity.setEmployeeId(request.employeeId());
        entity.setType(AbsenceType.valueOf(request.type()));
        entity.setFromDate(request.fromDate());
        entity.setToDate(request.toDate());
        entity.setNotes(request.notes());
        return absenceRepository.save(entity);
    }

    @Transactional
    public AbsenceEntity approve(UUID id) {
        AbsenceEntity entity = getById(id);
        if (entity.getStatus() != AbsenceStatus.PENDING) {
            throw new IllegalArgumentException("Only pending absences can be approved");
        }
        entity.setStatus(AbsenceStatus.APPROVED);
        return absenceRepository.save(entity);
    }

    @Transactional
    public AbsenceEntity reject(UUID id) {
        AbsenceEntity entity = getById(id);
        if (entity.getStatus() != AbsenceStatus.PENDING) {
            throw new IllegalArgumentException("Only pending absences can be rejected");
        }
        entity.setStatus(AbsenceStatus.REJECTED);
        return absenceRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<AbsenceEntity> findByEmployee(UUID employeeId) {
        return absenceRepository.findByEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public List<AbsenceEntity> findByDateRange(LocalDate from, LocalDate to) {
        return absenceRepository.findByDateRange(from, to);
    }

    private AbsenceEntity getById(UUID id) {
        return absenceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Absence not found: " + id));
    }
}
