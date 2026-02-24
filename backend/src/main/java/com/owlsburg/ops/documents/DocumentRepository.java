package com.owlsburg.ops.documents;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    Page<DocumentEntity> findByStatus(DocumentStatus status, Pageable pageable);

    Page<DocumentEntity> findByCategory(String category, Pageable pageable);

    Page<DocumentEntity> findByCategoryAndStatus(String category, DocumentStatus status, Pageable pageable);

    @Query("SELECT d FROM DocumentEntity d WHERE d.status = 'ACTIVE' AND (LOWER(d.title) LIKE LOWER(:term) OR LOWER(d.description) LIKE LOWER(:term))")
    List<DocumentEntity> searchByTitleOrDescription(@Param("term") String term);

    @Query("SELECT d FROM DocumentEntity d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%')) AND d.status = 'ACTIVE'")
    Page<DocumentEntity> searchByTitle(@Param("search") String search, Pageable pageable);
}
