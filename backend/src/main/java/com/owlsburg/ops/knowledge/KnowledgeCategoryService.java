package com.owlsburg.ops.knowledge;

import com.owlsburg.ops.knowledge.dto.KnowledgeCategoryRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeCategoryService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeCategoryService.class);

    private final KnowledgeCategoryRepository categoryRepository;

    public KnowledgeCategoryService(KnowledgeCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeCategoryEntity> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public KnowledgeCategoryEntity findById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Knowledge category not found: " + id));
    }

    @Transactional
    public KnowledgeCategoryEntity create(KnowledgeCategoryRequest request) {
        KnowledgeCategoryEntity entity = new KnowledgeCategoryEntity();
        entity.setName(request.name());
        entity.setColor(request.color());
        log.info("Creating knowledge category: {}", request.name());
        return categoryRepository.save(entity);
    }

    @Transactional
    public KnowledgeCategoryEntity update(UUID id, KnowledgeCategoryRequest request) {
        KnowledgeCategoryEntity entity = findById(id);
        entity.setName(request.name());
        entity.setColor(request.color());
        log.info("Updating knowledge category: {}", id);
        return categoryRepository.save(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Knowledge category not found: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Deleted knowledge category: {}", id);
    }
}
