package com.sindoflow.ops.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierPriceListRepository extends JpaRepository<SupplierPriceListEntity, UUID> {

    List<SupplierPriceListEntity> findBySupplierArticleId(UUID supplierArticleId);

    @Query("SELECT p FROM SupplierPriceListEntity p WHERE p.supplierArticleId = :supplierArticleId " +
            "AND p.validFrom <= :date AND (p.validUntil IS NULL OR p.validUntil >= :date) " +
            "ORDER BY p.validFrom DESC")
    Optional<SupplierPriceListEntity> findCurrentPrice(
            @Param("supplierArticleId") UUID supplierArticleId,
            @Param("date") LocalDate date);
}
