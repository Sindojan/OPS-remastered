package com.sindoflow.ops.people;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeShiftRepository extends JpaRepository<EmployeeShiftEntity, UUID> {

    List<EmployeeShiftEntity> findByEmployeeId(UUID employeeId);
}
