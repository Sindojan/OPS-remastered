package com.sindoflow.ops.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, UUID> {

    List<StockEntity> findByArticleId(UUID articleId);

    Optional<StockEntity> findByArticleIdAndWarehouseLocationId(UUID articleId, UUID warehouseLocationId);

    @Query("SELECT s FROM StockEntity s JOIN ArticleEntity a ON s.articleId = a.id " +
            "WHERE s.quantity < a.minStock")
    List<StockEntity> findCriticalStock();
}
