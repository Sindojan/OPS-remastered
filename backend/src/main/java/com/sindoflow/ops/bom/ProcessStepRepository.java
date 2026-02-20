package com.sindoflow.ops.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessStepRepository extends JpaRepository<ProcessStepEntity, UUID> {

    List<ProcessStepEntity> findByProcessPlanIdOrderByStepNumberAsc(UUID processPlanId);

    void deleteByProcessPlanId(UUID processPlanId);
}
