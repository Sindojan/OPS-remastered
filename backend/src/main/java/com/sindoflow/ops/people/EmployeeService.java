package com.sindoflow.ops.people;

import com.sindoflow.ops.people.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final EmployeeQualificationRepository qualificationRepository;
    private final EmployeeShiftRepository shiftRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmployeeQualificationRepository qualificationRepository,
                           EmployeeShiftRepository shiftRepository) {
        this.employeeRepository = employeeRepository;
        this.qualificationRepository = qualificationRepository;
        this.shiftRepository = shiftRepository;
    }

    @Transactional
    public EmployeeEntity create(CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmployeeNumber(request.employeeNumber())) {
            throw new IllegalArgumentException("Employee number already exists: " + request.employeeNumber());
        }
        EmployeeEntity entity = new EmployeeEntity();
        entity.setUserId(request.userId());
        entity.setEmployeeNumber(request.employeeNumber());
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        if (request.role() != null) {
            entity.setRole(request.role());
        }
        entity.setHireDate(request.hireDate());
        entity.setStationId(request.stationId());
        return employeeRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public EmployeeEntity getById(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeEntity> findAll(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    @Transactional
    public EmployeeEntity update(UUID id, UpdateEmployeeRequest request) {
        EmployeeEntity entity = getById(id);
        if (request.firstName() != null) entity.setFirstName(request.firstName());
        if (request.lastName() != null) entity.setLastName(request.lastName());
        if (request.email() != null) entity.setEmail(request.email());
        if (request.phone() != null) entity.setPhone(request.phone());
        if (request.role() != null) entity.setRole(request.role());
        if (request.status() != null) entity.setStatus(EmployeeStatus.valueOf(request.status()));
        if (request.hireDate() != null) entity.setHireDate(request.hireDate());
        if (request.stationId() != null) entity.setStationId(request.stationId());
        return employeeRepository.save(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new EntityNotFoundException("Employee not found: " + id);
        }
        employeeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<EmployeeEntity> getByStation(UUID stationId) {
        return employeeRepository.findByStationId(stationId);
    }

    @Transactional
    public EmployeeQualificationEntity addQualification(UUID employeeId, QualificationRequest request) {
        getById(employeeId); // verify exists
        EmployeeQualificationEntity entity = new EmployeeQualificationEntity();
        entity.setEmployeeId(employeeId);
        entity.setQualification(request.qualification());
        entity.setCertifiedAt(request.certifiedAt());
        entity.setExpiresAt(request.expiresAt());
        return qualificationRepository.save(entity);
    }

    @Transactional
    public void removeQualification(UUID employeeId, UUID qualificationId) {
        EmployeeQualificationEntity qual = qualificationRepository.findById(qualificationId)
                .orElseThrow(() -> new EntityNotFoundException("Qualification not found: " + qualificationId));
        if (!qual.getEmployeeId().equals(employeeId)) {
            throw new IllegalArgumentException("Qualification does not belong to employee");
        }
        qualificationRepository.delete(qual);
    }

    @Transactional(readOnly = true)
    public List<EmployeeQualificationEntity> getQualifications(UUID employeeId) {
        return qualificationRepository.findByEmployeeId(employeeId);
    }

    @Transactional
    public EmployeeShiftEntity assignShift(UUID employeeId, ShiftAssignmentRequest request) {
        getById(employeeId); // verify exists
        EmployeeShiftEntity entity = new EmployeeShiftEntity();
        entity.setEmployeeId(employeeId);
        entity.setShiftId(request.shiftId());
        entity.setValidFrom(request.validFrom());
        entity.setValidUntil(request.validUntil());
        return shiftRepository.save(entity);
    }
}
