package com.sindoflow.ops.inventory;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.inventory.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponse>> create(@Valid @RequestBody CreateArticleRequest request) {
        ArticleEntity entity = articleService.createArticle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ArticleResponse.from(entity)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getById(@PathVariable UUID id) {
        ArticleEntity entity = articleService.getArticleById(id);
        return ResponseEntity.ok(ApiResponse.ok(ArticleResponse.from(entity)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> findAll(Pageable pageable) {
        Page<ArticleResponse> page = articleService.findAllArticles(pageable).map(ArticleResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateArticleRequest request) {
        ArticleEntity entity = articleService.updateArticle(id, request);
        return ResponseEntity.ok(ApiResponse.ok(ArticleResponse.from(entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Article deleted"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> list = articleService.getAllCategories().stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        ArticleCategoryEntity entity = articleService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(CategoryResponse.from(entity)));
    }

    @GetMapping("/units")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> getUnits() {
        List<UnitResponse> list = articleService.getAllUnits().stream()
                .map(UnitResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/units")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(@Valid @RequestBody UnitRequest request) {
        UnitEntity entity = articleService.createUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(UnitResponse.from(entity)));
    }
}
