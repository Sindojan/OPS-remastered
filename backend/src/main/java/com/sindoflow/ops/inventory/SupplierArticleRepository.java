package com.sindoflow.ops.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierArticleRepository extends JpaRepository<SupplierArticleEntity, UUID> {

    List<SupplierArticleEntity> findBySupplierId(UUID supplierId);

    List<SupplierArticleEntity> findByArticleId(UUID articleId);
}
