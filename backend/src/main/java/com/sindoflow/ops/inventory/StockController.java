package com.sindoflow.ops.inventory;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.inventory.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;
    private final StockMovementService movementService;

    public StockController(StockService stockService, StockMovementService movementService) {
        this.stockService = stockService;
        this.movementService = movementService;
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ApiResponse<StockSummaryResponse>> getStock(@PathVariable UUID articleId) {
        StockSummaryResponse summary = stockService.getStock(articleId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/critical")
    public ResponseEntity<ApiResponse<List<CriticalArticleResponse>>> getCritical() {
        List<CriticalArticleResponse> critical = stockService.getCriticalArticles();
        return ResponseEntity.ok(ApiResponse.ok(critical));
    }

    @PostMapping("/movements")
    public ResponseEntity<ApiResponse<MovementResponse>> recordMovement(
            @Valid @RequestBody CreateMovementRequest request) {
        StockMovementType type = StockMovementType.valueOf(request.type());
        StockMovementEntity movement = movementService.recordMovement(
                request.articleId(), request.fromLocationId(), request.toLocationId(),
                request.quantity(), type, request.referenceType(), request.referenceId(),
                request.performedBy(), request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(MovementResponse.from(movement)));
    }
}
