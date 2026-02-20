package com.sindoflow.ops.people;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntryEntity, UUID> {

    Page<TimeEntryEntity> findByEmployeeIdAndTimestampBetween(
            UUID employeeId, Instant from, Instant to, Pageable pageable);

    List<TimeEntryEntity> findByEmployeeIdAndTimestampBetweenOrderByTimestampAsc(
            UUID employeeId, Instant from, Instant to);

    @Query("SELECT t FROM TimeEntryEntity t WHERE t.employeeId = :employeeId AND t.type = :type " +
            "ORDER BY t.timestamp DESC LIMIT 1")
    Optional<TimeEntryEntity> findLastByEmployeeIdAndType(
            @Param("employeeId") UUID employeeId,
            @Param("type") TimeEntryType type);

    @Query("SELECT t FROM TimeEntryEntity t WHERE t.employeeId = :employeeId " +
            "ORDER BY t.timestamp DESC LIMIT 1")
    Optional<TimeEntryEntity> findLastByEmployeeId(@Param("employeeId") UUID employeeId);
}
