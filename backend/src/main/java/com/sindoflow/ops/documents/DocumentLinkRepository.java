package com.sindoflow.ops.documents;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentLinkRepository extends JpaRepository<DocumentLinkEntity, UUID> {

    List<DocumentLinkEntity> findByDocumentId(UUID documentId);

    List<DocumentLinkEntity> findByLinkedTypeAndLinkedId(String linkedType, UUID linkedId);
}
