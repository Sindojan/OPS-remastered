package com.owlsburg.ops.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticleEntity, UUID> {

    Page<KnowledgeArticleEntity> findByStatus(ArticleStatus status, Pageable pageable);

    Page<KnowledgeArticleEntity> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<KnowledgeArticleEntity> findByStatusAndCategoryId(ArticleStatus status, UUID categoryId, Pageable pageable);

    Optional<KnowledgeArticleEntity> findBySlug(String slug);

    @Query("SELECT a FROM KnowledgeArticleEntity a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<KnowledgeArticleEntity> searchByTitle(@Param("search") String search, Pageable pageable);

    @Query("SELECT a FROM KnowledgeArticleEntity a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) AND a.status = :status")
    Page<KnowledgeArticleEntity> searchByTitleAndStatus(@Param("search") String search, @Param("status") ArticleStatus status, Pageable pageable);

    @Query("SELECT a FROM KnowledgeArticleEntity a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) AND a.categoryId = :categoryId")
    Page<KnowledgeArticleEntity> searchByTitleAndCategoryId(@Param("search") String search, @Param("categoryId") UUID categoryId, Pageable pageable);

    @Query("SELECT a FROM KnowledgeArticleEntity a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) AND a.status = :status AND a.categoryId = :categoryId")
    Page<KnowledgeArticleEntity> searchByTitleAndStatusAndCategoryId(@Param("search") String search, @Param("status") ArticleStatus status, @Param("categoryId") UUID categoryId, Pageable pageable);

    boolean existsBySlug(String slug);
}
