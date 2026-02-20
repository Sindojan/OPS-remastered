package com.sindoflow.ops.documents;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.documents.dto.DocumentLinkRequest;
import com.sindoflow.ops.documents.dto.DocumentLinkResponse;
import com.sindoflow.ops.documents.dto.DocumentResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy) {

        DocumentEntity doc = documentService.upload(file, title, description, category, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(DocumentResponse.from(doc), "Document uploaded"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> list(
            Pageable pageable,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status) {

        Page<DocumentResponse> page = documentService.findAll(pageable, category, status)
                .map(DocumentResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(@PathVariable UUID id) {
        DocumentEntity doc = documentService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(DocumentResponse.from(doc)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Void> download(@PathVariable UUID id) {
        String presignedUrl = documentService.getPresignedUrl(id, 15);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, presignedUrl)
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Document deleted"));
    }

    @PostMapping("/{id}/link")
    public ResponseEntity<ApiResponse<DocumentLinkResponse>> link(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentLinkRequest request) {

        DocumentLinkEntity link = documentService.link(id, request.linkedType(), request.linkedId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(DocumentLinkResponse.from(link), "Document linked"));
    }
}
