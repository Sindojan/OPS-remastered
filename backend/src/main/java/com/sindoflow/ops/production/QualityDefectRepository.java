package com.sindoflow.ops.production;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QualityDefectRepository extends JpaRepository<QualityDefectEntity, UUID> {
}
