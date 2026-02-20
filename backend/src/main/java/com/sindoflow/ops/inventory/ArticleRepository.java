package com.sindoflow.ops.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<ArticleEntity, UUID> {

    Optional<ArticleEntity> findByArticleNumber(String articleNumber);

    List<ArticleEntity> findByCategoryId(UUID categoryId);

    boolean existsByArticleNumber(String articleNumber);
}
