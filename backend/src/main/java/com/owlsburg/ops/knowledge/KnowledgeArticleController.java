package com.owlsburg.ops.knowledge;

import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.knowledge.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge/articles")
public class KnowledgeArticleController {

    private final KnowledgeArticleService articleService;

    public KnowledgeArticleController(KnowledgeArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<KnowledgeArticleSummaryResponse>>> list(
            Pageable pageable,
            @RequestParam(value = "status", required = false) ArticleStatus status,
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "search", required = false) String search) {

        Page<KnowledgeArticleSummaryResponse> page = articleService.findAllSummaries(pageable, status, categoryId, search);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeArticleResponse>> create(
            @Valid @RequestBody KnowledgeArticleCreateRequest request,
            Authentication authentication) {

        UUID authorId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            authorId = userId;
        }

        KnowledgeArticleResponse response = articleService.createAsResponse(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Article created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KnowledgeArticleResponse>> getById(@PathVariable UUID id) {
        KnowledgeArticleResponse response = articleService.findByIdAsResponse(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<KnowledgeArticleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody KnowledgeArticleUpdateRequest request) {

        KnowledgeArticleResponse response = articleService.updateAsResponse(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<KnowledgeArticleResponse>> publish(@PathVariable UUID id) {
        KnowledgeArticleResponse response = articleService.publishAsResponse(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Article published"));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<KnowledgeArticleResponse>> archive(@PathVariable UUID id) {
        KnowledgeArticleResponse response = articleService.archiveAsResponse(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Article archived"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        articleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Article deleted"));
    }
}
