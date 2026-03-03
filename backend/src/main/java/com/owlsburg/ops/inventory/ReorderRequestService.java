package com.owlsburg.ops.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ReorderRequestService {

    private static final Logger log = LoggerFactory.getLogger(ReorderRequestService.class);

    private final ReorderRequestRepository repository;

    public ReorderRequestService(ReorderRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ReorderRequestEntity create(UUID articleId, BigDecimal quantity,
                                        UUID supplierId, String notes, String requestedBy) {
        ReorderRequestEntity entity = new ReorderRequestEntity();
        entity.setArticleId(articleId);
        entity.setQuantity(quantity);
        entity.setSupplierId(supplierId);
        entity.setNotes(notes);
        entity.setRequestedBy(requestedBy);
        ReorderRequestEntity saved = repository.save(entity);
        log.info("Created reorder request {} for article {} qty {}", saved.getId(), articleId, quantity);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ReorderRequestEntity> findOpen() {
        return repository.findByStatus("OPEN");
    }

    @Transactional(readOnly = true)
    public List<ReorderRequestEntity> findByArticle(UUID articleId) {
        return repository.findByArticleId(articleId);
    }
}
