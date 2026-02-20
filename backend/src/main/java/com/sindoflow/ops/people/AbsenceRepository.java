package com.sindoflow.ops.people;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AbsenceRepository extends JpaRepository<AbsenceEntity, UUID> {

    List<AbsenceEntity> findByEmployeeId(UUID employeeId);

    @Query("SELECT a FROM AbsenceEntity a WHERE a.fromDate <= :to AND a.toDate >= :from")
    List<AbsenceEntity> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<AbsenceEntity> findByEmployeeIdAndStatus(UUID employeeId, AbsenceStatus status);
}
