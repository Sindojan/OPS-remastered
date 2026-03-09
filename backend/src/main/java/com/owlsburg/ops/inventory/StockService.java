package com.owlsburg.ops.inventory;

import com.owlsburg.ops.events.DomainEventService;
import com.owlsburg.ops.inventory.dto.CriticalArticleResponse;
import com.owlsburg.ops.inventory.dto.StockResponse;
import com.owlsburg.ops.inventory.dto.StockSummaryResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockRepository stockRepository;
    private final ArticleRepository articleRepository;
    private final DomainEventService domainEventService;

    public StockService(StockRepository stockRepository,
                        ArticleRepository articleRepository,
                        DomainEventService domainEventService) {
        this.stockRepository = stockRepository;
        this.articleRepository = articleRepository;
        this.domainEventService = domainEventService;
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
        if (criticalStocks.isEmpty()) {
            return List.of();
        }
        List<UUID> articleIds = criticalStocks.stream().map(StockEntity::getArticleId).distinct().toList();
        Map<UUID, ArticleEntity> articlesById = articleRepository.findAllById(articleIds)
                .stream().collect(Collectors.toMap(ArticleEntity::getId, Function.identity()));
        return criticalStocks.stream()
                .map(s -> {
                    ArticleEntity article = articlesById.get(s.getArticleId());
                    BigDecimal minStock = article != null ? article.getMinStock() : BigDecimal.ZERO;
                    return new CriticalArticleResponse(
                            s.getArticleId(),
                            article != null ? article.getName() : null,
                            article != null ? article.getArticleNumber() : null,
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
        StockEntity saved = stockRepository.save(stock);
        checkCriticalStock(saved);
        return saved;
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
        StockEntity saved = stockRepository.save(stock);
        checkCriticalStock(saved);
        return saved;
    }

    private void checkCriticalStock(StockEntity stock) {
        try {
            ArticleEntity article = articleRepository.findById(stock.getArticleId()).orElse(null);
            if (article != null && article.getMinStock() != null) {
                BigDecimal available = stock.getQuantity().subtract(stock.getReservedQuantity());
                if (available.compareTo(article.getMinStock()) < 0) {
                    domainEventService.publish("STOCK_CRITICAL", "article", stock.getArticleId(),
                            "{\"articleId\":\"" + stock.getArticleId() + "\",\"available\":" + available + ",\"minStock\":" + article.getMinStock() + "}");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check critical stock: {}", e.getMessage());
        }
    }
}
