package com.sindoflow.ops.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArticleCategoryRepository extends JpaRepository<ArticleCategoryEntity, UUID> {

    List<ArticleCategoryEntity> findByParentId(UUID parentId);

    List<ArticleCategoryEntity> findByParentIdIsNull();
}
