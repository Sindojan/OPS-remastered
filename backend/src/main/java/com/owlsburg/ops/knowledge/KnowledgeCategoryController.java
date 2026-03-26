package com.owlsburg.ops.knowledge;

import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.knowledge.dto.KnowledgeCategoryRequest;
import com.owlsburg.ops.knowledge.dto.KnowledgeCategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge/categories")
public class KnowledgeCategoryController {

    private final KnowledgeCategoryService categoryService;

    public KnowledgeCategoryController(KnowledgeCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgeCategoryResponse>>> list() {
        List<KnowledgeCategoryResponse> categories = categoryService.findAll().stream()
                .map(KnowledgeCategoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<KnowledgeCategoryResponse>> create(
            @Valid @RequestBody KnowledgeCategoryRequest request) {
        KnowledgeCategoryEntity entity = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(KnowledgeCategoryResponse.from(entity), "Category created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<KnowledgeCategoryResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody KnowledgeCategoryRequest request) {
        KnowledgeCategoryEntity entity = categoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(KnowledgeCategoryResponse.from(entity)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Category deleted"));
    }
}
