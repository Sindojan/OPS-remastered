package com.sindoflow.ops.bom;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.bom.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bom")
public class BomController {

    private final BomService bomService;

    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @PostMapping("/versions")
    public ResponseEntity<ApiResponse<BomVersionResponse>> createVersion(
            @Valid @RequestBody CreateBomVersionRequest request) {
        BomVersionEntity entity = bomService.createVersion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(BomVersionResponse.from(entity)));
    }

    @PostMapping("/versions/{id}/items")
    public ResponseEntity<ApiResponse<BomItemResponse>> addItem(
            @PathVariable UUID id, @Valid @RequestBody BomItemRequest request) {
        BomItemEntity entity = bomService.addItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(BomItemResponse.from(entity)));
    }

    @GetMapping("/versions/{id}/items")
    public ResponseEntity<ApiResponse<List<BomItemResponse>>> getItems(@PathVariable UUID id) {
        List<BomItemResponse> items = bomService.getItems(id).stream()
                .map(BomItemResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(@PathVariable UUID itemId) {
        bomService.removeItem(itemId);
        return ResponseEntity.ok(ApiResponse.ok(null, "BOM item removed"));
    }

    @PatchMapping("/versions/{id}/activate")
    public ResponseEntity<ApiResponse<BomVersionResponse>> activateVersion(@PathVariable UUID id) {
        BomVersionEntity entity = bomService.activateVersion(id);
        return ResponseEntity.ok(ApiResponse.ok(BomVersionResponse.from(entity)));
    }

    @GetMapping("/parts/{partId}/active")
    public ResponseEntity<ApiResponse<BomVersionResponse>> getActiveVersion(@PathVariable UUID partId) {
        BomVersionEntity entity = bomService.getActiveVersionForPart(partId);
        return ResponseEntity.ok(ApiResponse.ok(BomVersionResponse.from(entity)));
    }
}
