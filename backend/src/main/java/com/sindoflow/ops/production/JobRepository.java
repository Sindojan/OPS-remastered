package com.sindoflow.ops.production;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByJobNumber(String jobNumber);

    Page<JobEntity> findByStatus(JobStatus status, Pageable pageable);

    Page<JobEntity> findByCustomerId(UUID customerId, Pageable pageable);

    long countByAssignedStationIdAndShiftIdAndStatusIn(UUID stationId, UUID shiftId, java.util.Collection<JobStatus> statuses);
}
