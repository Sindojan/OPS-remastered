package com.sindoflow.ops.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessPlanRepository extends JpaRepository<ProcessPlanEntity, UUID> {

    List<ProcessPlanEntity> findByPartId(UUID partId);

    Optional<ProcessPlanEntity> findByPartIdAndStatus(UUID partId, VersionStatus status);
}
