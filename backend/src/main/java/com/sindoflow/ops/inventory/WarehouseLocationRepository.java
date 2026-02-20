package com.sindoflow.ops.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocationEntity, UUID> {

    List<WarehouseLocationEntity> findByWarehouseId(UUID warehouseId);
}
