package com.owlsburg.ops.people;

import com.owlsburg.ops.people.dto.AbsenceResponse;
import com.owlsburg.ops.people.dto.CreateAbsenceRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public List<AbsenceEntity> findAll() {
        return absenceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AbsenceEntity> findByEmployee(UUID employeeId) {
        return absenceRepository.findByEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public List<AbsenceEntity> findByDateRange(LocalDate from, LocalDate to) {
        return absenceRepository.findByDateRange(from, to);
    }

    @Transactional(readOnly = true)
    public List<AbsenceEntity> findByEmployeeAndStatus(UUID employeeId, AbsenceStatus status) {
        return absenceRepository.findByEmployeeIdAndStatus(employeeId, status);
    }

    @Transactional(readOnly = true)
    public List<AbsenceEntity> findByStatus(AbsenceStatus status) {
        return absenceRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<AbsenceResponse> toResponsesWithNames(List<AbsenceEntity> absences) {
        if (absences.isEmpty()) return List.of();
        List<UUID> employeeIds = absences.stream().map(AbsenceEntity::getEmployeeId).distinct().toList();
        Map<UUID, EmployeeEntity> employeesById = employeeRepository.findAllById(employeeIds)
                .stream().collect(Collectors.toMap(EmployeeEntity::getId, Function.identity()));
        return absences.stream()
                .map(a -> {
                    EmployeeEntity emp = employeesById.get(a.getEmployeeId());
                    return AbsenceResponse.from(a,
                            emp != null ? emp.getFirstName() : null,
                            emp != null ? emp.getLastName() : null);
                })
                .toList();
    }

    private AbsenceEntity getById(UUID id) {
        return absenceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Absence not found: " + id));
    }
}
