package com.sindoflow.ops.inventory;

import com.sindoflow.ops.inventory.dto.CriticalArticleResponse;
import com.sindoflow.ops.inventory.dto.StockResponse;
import com.sindoflow.ops.inventory.dto.StockSummaryResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockRepository stockRepository;
    private final ArticleRepository articleRepository;

    public StockService(StockRepository stockRepository,
                        ArticleRepository articleRepository) {
        this.stockRepository = stockRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public StockSummaryResponse getStock(UUID articleId) {
        List<StockEntity> stocks = stockRepository.findByArticleId(articleId);
        BigDecimal totalQty = stocks.stream()
                .map(StockEntity::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReserved = stocks.stream()
                .map(StockEntity::getReservedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<StockResponse> locations = stocks.stream()
                .map(StockResponse::from)
                .toList();
        return new StockSummaryResponse(
                articleId, totalQty, totalReserved,
                totalQty.subtract(totalReserved), locations
        );
    }

    @Transactional(readOnly = true)
    public StockResponse getStockByLocation(UUID articleId, UUID locationId) {
        StockEntity stock = stockRepository.findByArticleIdAndWarehouseLocationId(articleId, locationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for article " + articleId + " at location " + locationId));
        return StockResponse.from(stock);
    }

    @Transactional(readOnly = true)
    public List<CriticalArticleResponse> getCriticalArticles() {
        List<StockEntity> criticalStocks = stockRepository.findCriticalStock();
        return criticalStocks.stream()
                .map(s -> {
                    ArticleEntity article = articleRepository.findById(s.getArticleId()).orElse(null);
                    BigDecimal minStock = article != null ? article.getMinStock() : BigDecimal.ZERO;
                    return new CriticalArticleResponse(
                            s.getArticleId(),
                            s.getWarehouseLocationId(),
                            s.getQuantity(),
                            minStock,
                            minStock.subtract(s.getQuantity())
                    );
                })
                .toList();
    }

    @Transactional
    public StockEntity reserve(UUID articleId, UUID locationId, BigDecimal quantity) {
        StockEntity stock = stockRepository.findByArticleIdAndWarehouseLocationId(articleId, locationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for article " + articleId + " at location " + locationId));
        BigDecimal available = stock.getQuantity().subtract(stock.getReservedQuantity());
        if (available.compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Insufficient available stock. Available: " + available + ", requested: " + quantity);
        }
        stock.setReservedQuantity(stock.getReservedQuantity().add(quantity));
        return stockRepository.save(stock);
    }

    @Transactional
    public StockEntity release(UUID articleId, UUID locationId, BigDecimal quantity) {
        StockEntity stock = stockRepository.findByArticleIdAndWarehouseLocationId(articleId, locationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for article " + articleId + " at location " + locationId));
        if (stock.getReservedQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Cannot release more than reserved. Reserved: " + stock.getReservedQuantity());
        }
        stock.setReservedQuantity(stock.getReservedQuantity().subtract(quantity));
        return stockRepository.save(stock);
    }
}
