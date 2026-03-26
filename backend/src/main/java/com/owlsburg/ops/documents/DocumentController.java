package com.owlsburg.ops.documents;

import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.documents.dto.DocumentLinkRequest;
import com.owlsburg.ops.documents.dto.DocumentLinkResponse;
import com.owlsburg.ops.documents.dto.DocumentMetadataUpdateRequest;
import com.owlsburg.ops.documents.dto.DocumentResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAM_LEAD')")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID uploadedBy = (UUID) auth.getPrincipal();
        DocumentEntity doc = documentService.upload(file, title, description, category, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(DocumentResponse.from(doc), "Document uploaded"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> list(
            Pageable pageable,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search) {

        Page<DocumentResponse> page = documentService.findAll(pageable, category, status, search)
                .map(DocumentResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(@PathVariable UUID id) {
        DocumentEntity doc = documentService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(DocumentResponse.from(doc)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAM_LEAD')")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateMetadata(
            @PathVariable UUID id,
            @RequestBody DocumentMetadataUpdateRequest request) {

        DocumentEntity doc = documentService.updateMetadata(
                id, request.title(), request.description(), request.categoryId(), request.excerpt());
        return ResponseEntity.ok(ApiResponse.ok(DocumentResponse.from(doc)));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<Map<String, String>>> preview(@PathVariable UUID id) {
        String presignedUrl = documentService.getPresignedUrl(id, 15);
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("url", presignedUrl)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Void> download(@PathVariable UUID id) {
        String presignedUrl = documentService.getPresignedUrl(id, 15);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, presignedUrl)
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Document deleted"));
    }

    @PostMapping("/{id}/link")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAM_LEAD')")
    public ResponseEntity<ApiResponse<DocumentLinkResponse>> link(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentLinkRequest request) {

        DocumentLinkEntity link = documentService.link(id, request.linkedType(), request.linkedId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(DocumentLinkResponse.from(link), "Document linked"));
    }
}
