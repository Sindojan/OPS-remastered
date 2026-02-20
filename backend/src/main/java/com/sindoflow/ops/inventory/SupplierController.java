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
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponse>> create(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierEntity entity = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(SupplierResponse.from(entity)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> getById(@PathVariable UUID id) {
        SupplierEntity entity = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.ok(SupplierResponse.from(entity)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupplierResponse>>> findAll(Pageable pageable) {
        Page<SupplierResponse> page = supplierService.findAllSuppliers(pageable).map(SupplierResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateSupplierRequest request) {
        SupplierEntity entity = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.ok(SupplierResponse.from(entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Supplier deleted"));
    }

    @PostMapping("/{id}/articles")
    public ResponseEntity<ApiResponse<SupplierArticleResponse>> addArticle(
            @PathVariable UUID id, @Valid @RequestBody SupplierArticleRequest request) {
        SupplierArticleEntity entity = supplierService.addSupplierArticle(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(SupplierArticleResponse.from(entity)));
    }

    @GetMapping("/{id}/articles")
    public ResponseEntity<ApiResponse<List<SupplierArticleResponse>>> getArticles(@PathVariable UUID id) {
        List<SupplierArticleResponse> list = supplierService.getSupplierArticles(id).stream()
                .map(SupplierArticleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/{supplierId}/articles/{articleId}/prices")
    public ResponseEntity<ApiResponse<PriceListResponse>> addPrice(
            @PathVariable UUID supplierId,
            @PathVariable UUID articleId,
            @Valid @RequestBody PriceListRequest request) {
        // Find the supplier article by supplier and article
        List<SupplierArticleEntity> articles = supplierService.getSupplierArticles(supplierId);
        SupplierArticleEntity supplierArticle = articles.stream()
                .filter(sa -> sa.getArticleId().equals(articleId))
                .findFirst()
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Supplier article not found for supplier " + supplierId + " and article " + articleId));
        SupplierPriceListEntity entity = supplierService.addPriceList(supplierArticle.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(PriceListResponse.from(entity)));
    }
}
