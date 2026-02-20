package com.sindoflow.ops.documents;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    Page<DocumentEntity> findByStatus(DocumentStatus status, Pageable pageable);

    Page<DocumentEntity> findByCategory(String category, Pageable pageable);

    Page<DocumentEntity> findByCategoryAndStatus(String category, DocumentStatus status, Pageable pageable);
}
