package com.sindoflow.ops.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CostRateRepository extends JpaRepository<CostRateEntity, UUID> {

    @Query("SELECT c FROM CostRateEntity c WHERE c.type = :type AND c.referenceId = :referenceId " +
            "AND c.validFrom <= :date AND (c.validUntil IS NULL OR c.validUntil >= :date) " +
            "ORDER BY c.validFrom DESC")
    Optional<CostRateEntity> findCurrentRate(
            @Param("type") String type,
            @Param("referenceId") UUID referenceId,
            @Param("date") LocalDate date);
}
