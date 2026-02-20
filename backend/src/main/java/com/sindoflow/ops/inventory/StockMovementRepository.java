package com.sindoflow.ops.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovementEntity, UUID> {

    Page<StockMovementEntity> findByArticleId(UUID articleId, Pageable pageable);
}
