package com.sindoflow.ops.inventory;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class StockMovementService {

    private static final Logger log = LoggerFactory.getLogger(StockMovementService.class);

    private final StockMovementRepository movementRepository;
    private final StockRepository stockRepository;

    public StockMovementService(StockMovementRepository movementRepository,
                                StockRepository stockRepository) {
        this.movementRepository = movementRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public StockMovementEntity recordMovement(UUID articleId, UUID fromLocationId, UUID toLocationId,
                                               BigDecimal quantity, StockMovementType type,
                                               String referenceType, UUID referenceId,
                                               UUID performedBy, String notes) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Validate stock availability for OUTBOUND and TRANSFER
        if (type == StockMovementType.OUTBOUND || type == StockMovementType.TRANSFER) {
            if (fromLocationId == null) {
                throw new IllegalArgumentException("fromLocationId is required for " + type + " movements");
            }
            StockEntity fromStock = stockRepository.findByArticleIdAndWarehouseLocationId(articleId, fromLocationId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No stock record for article " + articleId + " at location " + fromLocationId));
            BigDecimal available = fromStock.getQuantity().subtract(fromStock.getReservedQuantity());
            if (available.compareTo(quantity) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available: " + available + ", requested: " + quantity);
            }
            // Decrease stock at source
            fromStock.setQuantity(fromStock.getQuantity().subtract(quantity));
            stockRepository.save(fromStock);
        }

        // Increase stock at destination for INBOUND and TRANSFER
        if (type == StockMovementType.INBOUND || type == StockMovementType.TRANSFER) {
            if (toLocationId == null) {
                throw new IllegalArgumentException("toLocationId is required for " + type + " movements");
            }
            StockEntity toStock = stockRepository.findByArticleIdAndWarehouseLocationId(articleId, toLocationId)
                    .orElseGet(() -> {
                        StockEntity newStock = new StockEntity();
                        newStock.setArticleId(articleId);
                        newStock.setWarehouseLocationId(toLocationId);
                        newStock.setQuantity(BigDecimal.ZERO);
                        newStock.setReservedQuantity(BigDecimal.ZERO);
                        return newStock;
                    });
            toStock.setQuantity(toStock.getQuantity().add(quantity));
            stockRepository.save(toStock);
        }

        // For CORRECTION, adjust the target location
        if (type == StockMovementType.CORRECTION) {
            UUID locationId = toLocationId != null ? toLocationId : fromLocationId;
            if (locationId == null) {
                throw new IllegalArgumentException("At least one location is required for CORRECTION movements");
            }
            StockEntity stock = stockRepository.findByArticleIdAndWarehouseLocationId(articleId, locationId)
                    .orElseGet(() -> {
                        StockEntity newStock = new StockEntity();
                        newStock.setArticleId(articleId);
                        newStock.setWarehouseLocationId(locationId);
                        newStock.setQuantity(BigDecimal.ZERO);
                        newStock.setReservedQuantity(BigDecimal.ZERO);
                        return newStock;
                    });
            // For correction: positive quantity = add, toLocationId used for increase, fromLocationId for decrease
            if (toLocationId != null && fromLocationId == null) {
                stock.setQuantity(stock.getQuantity().add(quantity));
            } else if (fromLocationId != null && toLocationId == null) {
                stock.setQuantity(stock.getQuantity().subtract(quantity));
            }
            stockRepository.save(stock);
        }

        // Record the immutable movement
        StockMovementEntity movement = new StockMovementEntity();
        movement.setArticleId(articleId);
        movement.setFromLocationId(fromLocationId);
        movement.setToLocationId(toLocationId);
        movement.setQuantity(quantity);
        movement.setType(type);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setPerformedBy(performedBy);
        movement.setNotes(notes);
        return movementRepository.save(movement);
    }
}
