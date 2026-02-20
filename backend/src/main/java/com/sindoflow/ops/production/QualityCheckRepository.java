package com.sindoflow.ops.production;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QualityCheckRepository extends JpaRepository<QualityCheckEntity, UUID> {

    List<QualityCheckEntity> findByJobId(UUID jobId);
}
