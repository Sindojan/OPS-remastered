package com.sindoflow.ops.people;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {

    Optional<EmployeeEntity> findByEmployeeNumber(String employeeNumber);

    List<EmployeeEntity> findByStationId(UUID stationId);

    List<EmployeeEntity> findByStatus(EmployeeStatus status);

    boolean existsByEmployeeNumber(String employeeNumber);
}
