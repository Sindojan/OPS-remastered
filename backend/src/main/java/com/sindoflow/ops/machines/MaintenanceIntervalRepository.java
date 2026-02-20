package com.sindoflow.ops.machines;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceIntervalRepository extends JpaRepository<MaintenanceIntervalEntity, UUID> {

    List<MaintenanceIntervalEntity> findByMachineId(UUID machineId);

    List<MaintenanceIntervalEntity> findByNextDueAtBefore(Instant threshold);

    List<MaintenanceIntervalEntity> findByNextDueAtBetween(Instant from, Instant to);
}
