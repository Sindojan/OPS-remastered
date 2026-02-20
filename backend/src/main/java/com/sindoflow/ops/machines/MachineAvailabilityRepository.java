package com.sindoflow.ops.machines;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MachineAvailabilityRepository extends JpaRepository<MachineAvailabilityEntity, UUID> {

    List<MachineAvailabilityEntity> findByMachineId(UUID machineId);
}
