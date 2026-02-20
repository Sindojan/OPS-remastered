package com.sindoflow.ops.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CalculationRepository extends JpaRepository<CalculationEntity, UUID> {

    List<CalculationEntity> findByPartId(UUID partId);
}
