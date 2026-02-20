package com.sindoflow.ops.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BomItemRepository extends JpaRepository<BomItemEntity, UUID> {

    List<BomItemEntity> findByBomVersionIdOrderByPositionAsc(UUID bomVersionId);

    void deleteByBomVersionId(UUID bomVersionId);
}
