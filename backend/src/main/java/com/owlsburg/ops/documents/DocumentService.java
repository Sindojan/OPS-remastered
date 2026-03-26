package com.owlsburg.ops.documents;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final DocumentStorageService storageService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentLinkRepository documentLinkRepository,
                           DocumentStorageService storageService) {
        this.documentRepository = documentRepository;
        this.documentLinkRepository = documentLinkRepository;
        this.storageService = storageService;
    }

    @Transactional
    public DocumentEntity upload(MultipartFile file, String title, String description,
                                 String category, UUID uploadedBy) {
        String originalName = file.getOriginalFilename();
        String safeName = originalName != null
                ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "upload";
        String fileKey = UUID.randomUUID() + "/" + safeName;

        try {
            storageService.upload(fileKey, file.getInputStream(), file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }

        DocumentEntity doc = new DocumentEntity();
        doc.setTitle(title);
        doc.setDescription(description);
        doc.setCategory(category);
        doc.setFileKey(fileKey);
        doc.setFileName(file.getOriginalFilename());
        doc.setMimeType(file.getContentType());
        doc.setFileSizeBytes(file.getSize());
        doc.setUploadedBy(uploadedBy);
        doc.setStatus(DocumentStatus.ACTIVE);
        doc.setVersion(1);

        log.info("Uploading document: {} ({})", title, fileKey);
        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public Page<DocumentEntity> findAll(Pageable pageable, String category, String status) {
        if (category != null && status != null) {
            return documentRepository.findByCategoryAndStatus(category, DocumentStatus.valueOf(status), pageable);
        }
        if (category != null) {
            return documentRepository.findByCategory(category, pageable);
        }
        if (status != null) {
            return documentRepository.findByStatus(DocumentStatus.valueOf(status), pageable);
        }
        return documentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public DocumentEntity findById(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + id));
    }

    @Transactional
    public void delete(UUID id) {
        DocumentEntity doc = findById(id);
        doc.setStatus(DocumentStatus.DELETED);
        documentRepository.save(doc);

        try {
            storageService.delete(doc.getFileKey());
        } catch (Exception e) {
            log.warn("Failed to delete file from storage: {}. Metadata marked as DELETED.", doc.getFileKey(), e);
        }

        log.info("Soft-deleted document: {}", id);
    }

    @Transactional
    public DocumentLinkEntity link(UUID documentId, String linkedType, UUID linkedId) {
        // Verify document exists
        findById(documentId);

        DocumentLinkEntity link = new DocumentLinkEntity();
        link.setDocumentId(documentId);
        link.setLinkedType(linkedType);
        link.setLinkedId(linkedId);

        log.info("Linking document {} to {} {}", documentId, linkedType, linkedId);
        return documentLinkRepository.save(link);
    }

    @Transactional
    public DocumentEntity updateMetadata(UUID id, String title, String description, UUID categoryId, String excerpt) {
        DocumentEntity doc = findById(id);
        if (title != null) {
            doc.setTitle(title);
        }
        if (description != null) {
            doc.setDescription(description);
        }
        doc.setCategoryId(categoryId);
        if (excerpt != null) {
            doc.setExcerpt(excerpt);
        }
        log.info("Updating document metadata: {}", id);
        return documentRepository.save(doc);
    }

    public String getPresignedUrl(UUID id, int expiryMinutes) {
        DocumentEntity doc = findById(id);
        return storageService.getPresignedUrl(doc.getFileKey(), expiryMinutes);
    }

    @Transactional(readOnly = true)
    public Page<DocumentEntity> findAll(Pageable pageable, String category, String status, String search) {
        if (search != null && !search.isBlank()) {
            return documentRepository.searchByTitle(search, pageable);
        }
        return findAll(pageable, category, status);
    }
}
