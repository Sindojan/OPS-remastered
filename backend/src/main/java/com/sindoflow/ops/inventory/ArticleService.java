package com.sindoflow.ops.inventory;

import com.sindoflow.ops.inventory.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    private final ArticleRepository articleRepository;
    private final ArticleCategoryRepository categoryRepository;
    private final UnitRepository unitRepository;

    public ArticleService(ArticleRepository articleRepository,
                          ArticleCategoryRepository categoryRepository,
                          UnitRepository unitRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
    }

    // --- Articles ---

    @Transactional
    public ArticleEntity createArticle(CreateArticleRequest request) {
        if (articleRepository.existsByArticleNumber(request.articleNumber())) {
            throw new IllegalArgumentException("Article number already exists: " + request.articleNumber());
        }
        ArticleEntity entity = new ArticleEntity();
        entity.setArticleNumber(request.articleNumber());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setCategoryId(request.categoryId());
        entity.setUnitId(request.unitId());
        if (request.minStock() != null) entity.setMinStock(request.minStock());
        if (request.reorderPoint() != null) entity.setReorderPoint(request.reorderPoint());
        if (request.status() != null) entity.setStatus(request.status());
        return articleRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public ArticleEntity getArticleById(UUID id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<ArticleEntity> findAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }

    @Transactional
    public ArticleEntity updateArticle(UUID id, UpdateArticleRequest request) {
        ArticleEntity entity = getArticleById(id);
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.categoryId() != null) entity.setCategoryId(request.categoryId());
        if (request.unitId() != null) entity.setUnitId(request.unitId());
        if (request.minStock() != null) entity.setMinStock(request.minStock());
        if (request.reorderPoint() != null) entity.setReorderPoint(request.reorderPoint());
        if (request.status() != null) entity.setStatus(request.status());
        return articleRepository.save(entity);
    }

    @Transactional
    public void deleteArticle(UUID id) {
        if (!articleRepository.existsById(id)) {
            throw new EntityNotFoundException("Article not found: " + id);
        }
        articleRepository.deleteById(id);
    }

    // --- Categories ---

    @Transactional
    public ArticleCategoryEntity createCategory(CategoryRequest request) {
        ArticleCategoryEntity entity = new ArticleCategoryEntity();
        entity.setName(request.name());
        entity.setParentId(request.parentId());
        return categoryRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ArticleCategoryEntity> getAllCategories() {
        return categoryRepository.findAll();
    }

    // --- Units ---

    @Transactional
    public UnitEntity createUnit(UnitRequest request) {
        if (unitRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Unit name already exists: " + request.name());
        }
        if (unitRepository.existsByAbbreviation(request.abbreviation())) {
            throw new IllegalArgumentException("Unit abbreviation already exists: " + request.abbreviation());
        }
        UnitEntity entity = new UnitEntity();
        entity.setName(request.name());
        entity.setAbbreviation(request.abbreviation());
        return unitRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<UnitEntity> getAllUnits() {
        return unitRepository.findAll();
    }
}
